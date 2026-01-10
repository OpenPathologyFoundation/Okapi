# Okapi Echo Module

---

### **1. The UI Component: The "Echo" Anchor**

You requested "semi-conspicuous in the left corner."

* **Location:** Bottom-Left of the Global Navigation Rail. This is "Prime Real Estate" for persistent tools but stays out of the way of the high-value diagnostic pixels in the center.
* **Visual:** A small, circular icon.
* *Iconography:* Not a generic "Help" question mark (`?`). Use a **"Speech Bubble with a Pulse"** or a **"Feedback Loop"** icon.
* *State:* It is usually gray (inactive). However, if the system detects "Rage Clicking" (rapid clicking in one spot) or a system error, the icon gently pulses amber, proactively asking, *"Something wrong?"*


* **Hotkey:** `F1` or `~` (Tilde). Speed is key.

---

### **2. The Interaction: The "Context Capsule"**

When the user clicks the icon (or hits the hotkey), the screen does **not** navigate away.

1. **The Snapshot (Invisible):** The system immediately freezes the state metadata:
* *Case ID:* #12345
* *Viewport:* 40x Zoom, Coordinates X/Y.
* *Active Layers:* AI Heatmap ON, Annotations OFF.
* *System Load:* Latency 40ms.


2. **The Modal (Visible):** A compact, semi-transparent "glass" card slides out from the bottom left (overlaying the rail, not the image).

#### **The Dialogue Interface (The "Linguistic Component")**

Instead of a form with "Subject" and "Description," it is a **single input field** with a microphone icon (Voice-to-Text is crucial here).

* **User Action:** The user types or speaks:
* *"I wish I could compare this section to the frozen section from yesterday side-by-side without losing my place."*



* **System Response (LLM Processing):** The system analyzes the intent and the context. It replies:
* *"Captured. I've tagged this as a **Feature Request** for 'Synchronized Multi-Slide View.' I've also noted you are currently viewing Case #882."*


* **User Action:** Click "Submit" (or just hit Enter).

**Why this works:** The user didn't have to know the technical term "Synchronized View." They just expressed a desire.

---

### **3. The Evolution: Technical vs. Clinical**

You mentioned this technology might be "transient" or evolve. Here is how we design it to handle that ambiguity:

#### **Phase 1: The "Wishlist" & "Bug" Catcher (Technical Focus)**

The LLM acts as a Junior PM.

* *User:* "Why is this heatmap red? It's obviously fibrosis."
* *System:* "Feedback logged: **AI Discrepancy**. I've captured the region of interest where you disagreed with the model. This will be sent to the ML training team."

#### **Phase 2: The "Clinical Scribe" (Operational Focus)**

As users get used to talking to the corner of the screen, we pivot the utility.

* *User:* "This area looks weird, remind me to show Dr. Smith."
* *System:* "Flagged as **Clinical Note**. I've added a digital sticky note to this coordinate: 'Review with Dr. Smith'."

---

### **4. Design Artifact: The "Feedback Modal" Wireframe**

```text
+-------------------------------------------------------+
|  [Echo Feedback]                            [ X ]     |
+-------------------------------------------------------+
|  HISTORY:                                             |
|  [System]: Screenshot & State Logs attached.          |
|                                                       |
|  [User]: "Make the measuring tool stickier."          |
|                                                       |
|  [System]: Clarifying: Do you mean you want it to     |
|            snap to tissue edges automatically?        |
|                                                       |
|  [User]: "Yes, exactly."                              |
+-------------------------------------------------------+
|  > Type or hold [Space] to speak...             [ ^ ] |
+-------------------------------------------------------+
|  Suggested Tags: [Bug] [Wishlist] [Data Error]        |
+-------------------------------------------------------+

```

### **5. The "Feedback Loop" Dashboard (For You/Admins)**

Since you want usage monitoring, you need a view to digest this.

* **The "Frustration Heatmap":** Don't just list tickets. Show a visual representation of the UI. If 50 users submit feedback while using the "Stain Order" menu, that menu glows red on your admin dashboard.
* **The "Wishlist Cloud":** The LLM summarizes the free-text feedback into clusters:
* *Cluster A:* "Search is too strict" (15 users)
* *Cluster B:* "Need dark mode for reports" (8 users)
* *Cluster C:* "AI missed micro-mets" (3 users - **Critical**)



### **Summary of Benefits**

1. **Low Friction:** "Complaint as a Service."
2. **High Context:** You never get "It doesn't work" tickets. You get "It failed at 2:00 PM on Case X."
3. **Therapeutic:** It gives the user a feeling of control over the tool ("I told the system what I want").


This approach makes perfect sense. By treating the "separation of concerns" (clinical vs. technical) as a backend problem for the LLM to sort out later, we keep the frontend **radically simple**. The user just has "something to say," and they say it.

Here is the design proposal for the **"Okapi Echo"** linguistic interface, focusing on visual style and interaction.

---

### **The Visual Style: "The Glass Scribe"**

Since the application is in "Clinical Dark Mode" (Gunmetal/Charcoal), this component needs to feel lightweight but distinct. We will use **Glassmorphism** (frosted blur) to ensure it feels like it floats *above* the clinical work, capturing thoughts without obscuring the diagnosis.

#### **1. The Anchor (Collapsed State)**

* **Location:** Bottom-Left corner of the screen (fixed in the rail).
* **Shape:** A perfect circle (48x48px).
* **Color:** A translucent dark glass background with a **Warm Amber** accent (`#F59E0B`). We use Amber because it feels "attentive" and "waiting," distinct from the Clinical Blues (stability) and Red/Greens (alerts) of the main UI.
* **Icon:** A combined icon: A **Microphone** merged with a **Text Cursor** (`|`). This visually cues "Speak or Type."
* **Animation:** A very slow, rhythmic "breathing" glow (opacity shift 80% to 100%) helps the user find it peripherally without it being distracting.

#### **2. The Interface (Active/Expanded State)**

When activated (Click or Hotkey `~`), it doesn't open a rigid rectangular window. It blooms upward from the icon.

* **Container:** A "frosted glass" capsule. It blurs the background behind it (the slide or worklist) so the text is legible, but the context is still visible underneath.
* **Typography:** Large, monospaced font (like `JetBrains Mono` or `Roboto Mono`). It should look like a terminal or a script, reinforcing that this is a direct line to the system's "brain."
* **The Input Field:** There is no "Submit" button visible initially. Just a clean line waiting for input.

---

### **The Interaction Flow (The "Linguistic" Experience)**

This interface needs to feel faster than thinking.

#### **Mode A: Voice (The "Dictaphone" Feel)**

1. **Trigger:** User holds the Spacebar (Push-to-Talk) or clicks the Mic icon.
2. **Visual:** The input line transforms into a **live audio waveform**.
* *Design Detail:* The waveform reacts to the user's volume. It glows Amber.


3. **Feedback:** As the user speaks *"The segmentation on the cell boundaries is too fuzzy here,"* the text streams onto the screen in real-time (streaming transcription).
4. **Release:** User releases Spacebar. The system flashes a small "check" icon. "Logged." The glass capsule fades away.

#### **Mode B: Typing (The "Console" Feel)**

1. **Trigger:** User hits `~` or clicks the icon.
2. **Visual:** Focus is immediately in the text field.
3. **Action:** User types: `search is slow on patient id 444` -> Hits `Enter`.
4. **Reaction:** The box doesn't just disappear. It replies briefly using the LLM to confirm comprehension.
* *User:* "Search is slow..."
* *System (fade text):* "Performance issue noted on Search. Context captured." -> *Fade out.*



---

### **Visualizing the Component**

Here is a conceptual wireframe of the "Active" state in the bottom left corner.

```text
+------------------------------------------------------------
|  [ Main Pathology Viewer Content Is Visible But Blurred Here ]
|
|  +-------------------------------------------------------+
|  |  ( ) Auto-Context: Case #1293 | Zoom: 40x | AI: ON    |  <-- Subtle Meta-data
|  +-------------------------------------------------------+
|  |                                                       |
|  |  "The heatmap is obscuring the mitosis..."            |  <-- User Input (Large)
|  |  _________________________________________            |
|  |                                                       |
|  |  [ ||||||||||||||||| ]  Listening...                  |  <-- Audio Waveform
|  +------------------+------------------------------------+
|                     |
|  [ (Amber Icon) ] --+  <-- Anchor Point
|
+------------------------------------------------------------

```

### **Why this design works:**

1. **No Cognitive Load:** The user doesn't have to categorize (is this a bug? a feature?). They just speak.
2. **The "Amber" distinction:** By using a unique color (Amber) for this tool, we condition the user: *Blue is for the Patient, Amber is for the System.*
3. **The "Linguistic" payoff:** The monospaced font and waveform visualization reinforce that this is a conversation with the machine, not a form submission to a generic IT department.