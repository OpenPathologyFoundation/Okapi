-- Add case_uuid column to worklist for linking to wsi.cases
ALTER TABLE public.pathology_worklist ADD COLUMN case_uuid uuid NULL;
CREATE INDEX ix_worklist_case_uuid ON public.pathology_worklist (case_uuid);
