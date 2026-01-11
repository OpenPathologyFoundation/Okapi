#!/usr/bin/env bash
set -euo pipefail

: "${KEYCLOAK_CONTAINER:=okapi-keycloak}"
: "${KEYCLOAK_INTERNAL_URL:=http://localhost:8080}"   # URL as seen from inside the container
: "${KEYCLOAK_ADMIN_REALM:=master}"
: "${KEYCLOAK_REALM:=okapi}"

: "${DEMO_IDENTITIES_JSON:=seed/identities/demo-identities.v1.json}"
: "${DEMO_PASSWORD_DEFAULT:=OkapiDev!2026}"           # dev only; set via .env, do not commit secrets
: "${OUT_DIR:=seed/keycloak/out}"

# NOTE:
# - This script runs `kcadm.sh` inside the Keycloak container, but it runs JSON parsing on the host.
# - macOS typically has `python3` (not `python`). We intentionally use `python3` below.

KCADM="/opt/keycloak/bin/kcadm.sh"

: "${DOTENV_FILE:=auth-system/.env}"

# Optional: load environment defaults from a local `.env` file.
# This is useful because `docker compose` reads `.env` automatically, but your shell does not.
if [[ -z "${SKIP_DOTENV:-}" && -f "${DOTENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${DOTENV_FILE}"
  set +a
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker is required to run this script." >&2
  exit 1
fi

if ! docker inspect "${KEYCLOAK_CONTAINER}" >/dev/null 2>&1; then
  echo "ERROR: Keycloak container '${KEYCLOAK_CONTAINER}' not found." >&2
  echo "Start it first (from auth-system/): docker compose up -d keycloak" >&2
  exit 1
fi

if [[ "$(docker inspect -f '{{.State.Running}}' "${KEYCLOAK_CONTAINER}" 2>/dev/null)" != "true" ]]; then
  echo "ERROR: Keycloak container '${KEYCLOAK_CONTAINER}' is not running." >&2
  echo "Start it first (from auth-system/): docker compose up -d keycloak" >&2
  exit 1
fi

if ! docker exec -i "${KEYCLOAK_CONTAINER}" sh -lc 'test -n "${KEYCLOAK_ADMIN_PASSWORD:-}"' >/dev/null 2>&1; then
  echo "ERROR: KEYCLOAK_ADMIN_PASSWORD is not set inside container '${KEYCLOAK_CONTAINER}'." >&2
  echo "If you use docker compose, set it under keycloak.environment (see auth-system/docker-compose.yml)." >&2
  exit 1
fi

if [[ ! -f "${DEMO_IDENTITIES_JSON}" ]]; then
  echo "ERROR: identities seed file not found: ${DEMO_IDENTITIES_JSON}" >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "ERROR: python3 is required on the host to run this script." >&2
  echo "Install it (e.g., via Homebrew) or adjust the script to use your Python executable." >&2
  exit 1
fi

mkdir -p "${OUT_DIR}"

# Avoid accumulating duplicate lines across runs.
: > "${OUT_DIR}/keycloak-user-map.tsv"

echo "Authenticating kcadm inside container ${KEYCLOAK_CONTAINER}..."
docker exec "${KEYCLOAK_CONTAINER}" sh -lc "
  ${KCADM} config credentials \
    --server '${KEYCLOAK_INTERNAL_URL}' \
    --realm '${KEYCLOAK_ADMIN_REALM}' \
    --user \"\${KEYCLOAK_ADMIN:-admin}\" \
    --password \"\${KEYCLOAK_ADMIN_PASSWORD}\"
"

# Helper: get exact group id by name
get_group_id() {
  local gname="$1"
  # NOTE: We intentionally capture stderr (2>&1) because `kcadm.sh` prints many failures
  # (auth/realm/arg issues) to stderr, which would otherwise make the JSON parser see an
  # empty stream and crash with `JSONDecodeError`.
  docker exec "${KEYCLOAK_CONTAINER}" sh -lc "
    ${KCADM} get groups -r '${KEYCLOAK_REALM}' -q search='${gname}'
  " 2>&1 | python3 -c 'import json,sys
gname=sys.argv[1]
raw=sys.stdin.read().strip()
if not raw:
    print("")
    raise SystemExit(0)
try:
    arr=json.loads(raw)
except Exception:
    raise SystemExit("ERROR: Expected JSON from `kcadm.sh get groups`, but received:\n"+raw+"\n")
for g in arr:
    if g.get("name")==gname:
        print(g.get("id",""))
        raise SystemExit(0)
print("")
' "$gname"
}

# Helper: get user id by username
get_user_id() {
  local uname="$1"
  docker exec "${KEYCLOAK_CONTAINER}" sh -lc "
    ${KCADM} get users -r '${KEYCLOAK_REALM}' -q username='${uname}'
  " 2>&1 | python3 -c 'import json,sys
uname=sys.argv[1]
raw=sys.stdin.read().strip()
if not raw:
    print("")
    raise SystemExit(0)
try:
    arr=json.loads(raw)
except Exception:
    raise SystemExit("ERROR: Expected JSON from `kcadm.sh get users`, but received:\n"+raw+"\n")
for u in arr:
    if u.get("username")==uname:
        print(u.get("id",""))
        raise SystemExit(0)
print("")
' "$uname"
}

# Extract unique groups from JSON
echo "Ensuring groups exist..."
python3 - "${DEMO_IDENTITIES_JSON}" <<'PY' | while read -r g; do
import json, sys
path = sys.argv[1]
data = json.load(open(path))
groups = set()
for ident in data.get("identities", []):
    for g in ident.get("idp_groups", []):
        groups.add(g)
for g in sorted(groups):
    print(g)
PY
  gid="$(get_group_id "$g")"
  if [[ -z "$gid" ]]; then
    echo "  Creating group: $g"
    docker exec "${KEYCLOAK_CONTAINER}" sh -lc "
      ${KCADM} create groups -r '${KEYCLOAK_REALM}' -s name='${g}'
    " >/dev/null
  else
    echo "  Group exists: $g ($gid)"
  fi
done

# Seed users
echo "Seeding users..."
python3 - "${DEMO_IDENTITIES_JSON}" <<'PY' | while IFS=$'\t' read -r username email display_full display_short given_name family_name middle_name middle_initial nickname prefix suffix groups_csv; do
import json, sys
path = sys.argv[1]
data = json.load(open(path))
for ident in data.get("identities", []):
    username = ident["username"]
    email = ident.get("email","")
    display = (ident.get("display") or {}).get("full") or ident.get("display_name", "")
    display_short = (ident.get("display") or {}).get("short") or ""
    name = ident.get("name") or {}
    given_name = name.get("given_name") or ""
    family_name = name.get("family_name") or ""
    middle_name = name.get("middle_name") or ""
    middle_initial = name.get("middle_initial") or ""
    nickname = name.get("nickname") or ""
    prefix = name.get("prefix") or ""
    suffix = name.get("suffix") or ""
    groups = ident.get("idp_groups", [])
    print(
        username,
        email,
        display,
        display_short,
        given_name,
        family_name,
        middle_name,
        middle_initial,
        nickname,
        prefix,
        suffix,
        ",".join(groups),
        sep="\t",
    )
PY
  uid="$(get_user_id "$username")"

  # Prefer structured fields; fall back to a basic split.
  if [[ -n "${given_name}" ]]; then
    first="${given_name}"
  else
    first="${display_full%% *}"
  fi

  if [[ -n "${family_name}" ]]; then
    last="${family_name}"
  else
    last="${display_full#"$first"}"
    last="${last# }"
  fi

  if [[ -z "$uid" ]]; then
    echo "  Creating user: $username"
    docker exec "${KEYCLOAK_CONTAINER}" sh -lc "
      ${KCADM} create users -r '${KEYCLOAK_REALM}' \
        -s username='${username}' \
        -s enabled=true \
        -s email='${email}' \
        -s emailVerified=true \
        -s firstName='${first}' \
        -s lastName='${last}' \
        -s 'attributes.display_name=[\"${display_full}\"]' \
        -s 'attributes.display_short=[\"${display_short}\"]' \
        -s 'attributes.given_name=[\"${given_name}\"]' \
        -s 'attributes.family_name=[\"${family_name}\"]' \
        -s 'attributes.middle_name=[\"${middle_name}\"]' \
        -s 'attributes.middle_initial=[\"${middle_initial}\"]' \
        -s 'attributes.nickname=[\"${nickname}\"]' \
        -s 'attributes.prefix=[\"${prefix}\"]' \
        -s 'attributes.suffix=[\"${suffix}\"]'
    " >/dev/null
    uid="$(get_user_id "$username")"
  else
    echo "  User exists: $username ($uid)"
  fi

  # Set password (idempotent for dev)
  docker exec "${KEYCLOAK_CONTAINER}" sh -lc "
    ${KCADM} set-password -r '${KEYCLOAK_REALM}' \
      --username '${username}' \
      --new-password '${DEMO_PASSWORD_DEFAULT}'
  " >/dev/null || true

  # Add to groups
  groups=()
  IFS=',' read -ra groups <<< "${groups_csv:-}"
  for g in "${groups[@]:-}"; do
    [[ -z "$g" ]] && continue
    gid="$(get_group_id "$g")"
    if [[ -n "$gid" ]]; then
      # Add group membership (safe to re-run; ignore conflicts)
      docker exec "${KEYCLOAK_CONTAINER}" sh -lc "
        ${KCADM} update 'users/${uid}/groups/${gid}' -r '${KEYCLOAK_REALM}' -n
      " >/dev/null || true
    fi
  done

  echo "  OK: $username -> $uid"
  echo "$username	$uid" >> "${OUT_DIR}/keycloak-user-map.tsv"
done

echo "Done. Wrote: ${OUT_DIR}/keycloak-user-map.tsv"
echo "All users use DEMO_PASSWORD_DEFAULT (dev-only)."