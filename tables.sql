-- Table: public."naiveDecayStrategy"

-- DROP TABLE public."naiveDecayStrategy";

CREATE TABLE public."naiveDecayStrategy"
(
  id integer NOT NULL DEFAULT nextval('"naiveDecayStrategy_id_seq"'::regclass),
  "decayRate" double precision NOT NULL,
  CONSTRAINT "naiveDecayStrategy_pkey" PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."naiveDecayStrategy"
  OWNER TO representer;


-- Table: public.results

-- DROP TABLE public.results;

CREATE TABLE public.results
(
  id integer NOT NULL DEFAULT nextval('result_id_seq'::regclass),
  "tpTrain" integer NOT NULL,
  "fpTrain" integer NOT NULL,
  "tnTrain" integer NOT NULL,
  "fnTrain" integer NOT NULL,
  epoch integer NOT NULL,
  "tpTest" integer NOT NULL,
  "fpTest" integer NOT NULL,
  "tnTest" integer NOT NULL,
  "fnTest" integer NOT NULL,
  CONSTRAINT result_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public.results
  OWNER TO representer;


-- Table: public.runs

-- DROP TABLE public.runs;

CREATE TABLE public.runs
(
  model text NOT NULL,
  "firstHiddenLayerSize" integer NOT NULL,
  "initialLearningRate" double precision NOT NULL,
  "finalResult" integer,
  id integer NOT NULL DEFAULT nextval('results_id_seq'::regclass),
  "naiveDecayStrategy" integer,
  "trainingTimeNs" bigint NOT NULL,
  "testCaseName" text,
  "sampleSize" integer NOT NULL,
  CONSTRAINT results_pkey PRIMARY KEY (id),
  CONSTRAINT "results_finalResult_fkey" FOREIGN KEY ("finalResult")
      REFERENCES public.results (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "runs_naiveDecayStrategy_fkey" FOREIGN KEY ("naiveDecayStrategy")
      REFERENCES public."naiveDecayStrategy" (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public.runs
  OWNER TO representer;

-- View: public."resultView"

-- DROP VIEW public."resultView";

CREATE OR REPLACE VIEW public."resultView" AS
 SELECT runs.model,
    runs."firstHiddenLayerSize",
    runs."initialLearningRate",
    runs."finalResult",
    runs.id,
    runs."naiveDecayStrategy",
    runs."trainingTimeNs",
    runs."testCaseName",
    runs."sampleSize",
    f1(prec => prec(tp => results."tpTest"::double precision, fp => results."fpTest"::double precision), recall => recall(tp => results."tpTest"::double precision, fn => results."fnTest"::double precision)) AS f1test
   FROM runs,
    results
  WHERE runs."finalResult" = results.id;

ALTER TABLE public."resultView"
  OWNER TO representer;
