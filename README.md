# Sanaphor - semantic anaphora resolution

## System

1. IntermediateCoreferenceSystem.java generates a **mentions file**, containing all mentions extracted by *Stanford Coref* + their attributes. The file has the following format:

        DOC_ID  PARAGRAPH_ID    SENT_ID MENTION_ID      START_INDEX     END_INDEX       MENTION NER_ENTITY      HEAD_WORD_LEMMA HEAD_POS_TAG    COREF_ID        GOLD_COREF_ID   NER_ENTITY_TAG  POS_TAG_SEQ
        bc/cctv/00/cctv_0000    0       0       10099   9       10      people  null    people  NNS     10099   -1      O       NNS
        bc/cctv/00/cctv_0000    0       0       23      23      25      Hong Kong       Hong Kong       Kong    NNP     23      23      GPE     NNP NNP
2. At the same time the system generates two more files in the CoNLL format:
  1. Gold coreferences file, which contains gold coreference resolutions;
  2. Stanford coreferences file, which contains coreferences produced by the *Stanford Coref System*.

  These files can be used by the official CoNLL scorer script to evaluate coreference resultion systems.
3. Next, we link entities from the mentions file using state-of-the-art Entity Linking system, which produces the following **linked mentions file**: 

        DOC_ID  PARAGRAPH_ID    SENT_ID MENTION_ID      START_INDEX     END_INDEX       MENTION NER_ENTITY      HEAD_WORD_LEMMA HEAD_POS_TAG    COREF_ID        GOLD_COREF_ID   NER_ENTITY_TAG  POS_TAG_SEQ     null
        bc/cctv/00/cctv_0000    0       0       10099   9       10      people  null    people  NNS     10099   -1      O       NNS     null
        bc/cctv/00/cctv_0000    0       0       23      23      25      Hong Kong       Hong Kong       Kong    NNP     23      23      GPE     NNP NNP http://dbpedia.org/resource/Hong_Kong
4. Finally, we run sanaphor.py on the **linked mention** mentions, this package implements methods described in the paper. It produces modified file in the CoNLL format, that can be evaluated using CoNLL scorer as usual.

## CoNLL Scorer

Download from: https://github.com/conll/reference-coreference-scorers

Running:

    ~/reference-coreference-scorers/v8.01/scorer.pl all conll-test.predicted.new.txt conll-test-gold.txt

## Extra

### Data
Ontonotes 5.0 - https://catalog.ldc.upenn.edu/LDC2013T19

Generate CoNLL files from Skeleton files:

    ~/conll-2012/v3/scripts/skeleton2conll.sh -D ~/ontonotes-release-5.0/data/files/data ~/conll-2012/

### Stanford coreNLP shell

    /Library/Internet\ Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/bin/java -cp "*" -Xmx2g edu.stanford.nlp.pipeline.StanfordCoreNLP -annotators tokenize,ssplit,pos,lemma,ner,parse,dcoref
