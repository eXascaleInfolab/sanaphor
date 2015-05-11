# Sanaphor - semantic anaphora resolution

## System

1. IntermediateCoreferenceSystem.java generates the **mentions file**, which contains all mentions extracted by *Stanford Coref* + their attributes. File looks like the following:

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
