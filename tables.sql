

-- Table: public.runs

-- DROP TABLE public.runs;

CREATE TABLE runs
(
  id integer NOT NULL PRIMARY KEY,
  "testCaseName" text NOT NULL,
  "sampleSize" integer NOT NULL,
  "firstHiddenLayerSize" integer NOT NULL,
  "initialLearningRate" double precision NOT NULL,
  "learningRateDecayStrategy" text NOT NULL,
  "learningRateDecayRate" double precision,
  "trainingTimeNs" bigint NOT NULL,
  "lastEpoch" integer NOT NULL,
  "tpTrain" integer NOT NULL,
  "fpTrain" integer NOT NULL,
  "tnTrain" integer NOT NULL,
  "fnTrain" integer NOT NULL,
  "tpTest" integer NOT NULL,
  "fpTest" integer NOT NULL,
  "tnTest" integer NOT NULL,
  "fnTest" integer NOT NULL,
  model text NOT NULL,
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public.runs
  OWNER TO representer;

-- View: public."resultView"

-- DROP VIEW public."resultView";

CREATE OR REPLACE VIEW "resultView" AS
 SELECT *,
    f1(prec => prec(tp => runs."tpTest", fp => runs."fpTest"), recall => recall(tp => runs."tpTest", fn => runs."fnTest")) AS f1test
   FROM runs;

ALTER TABLE public."resultView"
  OWNER TO representer;
