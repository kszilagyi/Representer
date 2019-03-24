

-- Table: public.runs

-- DROP TABLE public.runs;

CREATE TABLE runs
(
  id SERIAL NOT NULL PRIMARY KEY,
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
  model text NOT NULL
)


-- View: public."resultView"

-- DROP VIEW public."resultView";

CREATE OR REPLACE VIEW "resultView" AS
 SELECT *,
    f1(prec => prec(tp => "tpTrain", fp => "fpTrain"), recall => recall(tp => "tpTrain", fn => "fnTrain")) AS "f1Train",
    f1(prec => prec(tp => "tpTest", fp => "fpTest"), recall => recall(tp => "tpTest", fn => "fnTest")) AS "f1test"
   FROM runs;

