package info.exascale.coref;

import edu.stanford.nlp.classify.LogisticClassifier;
import edu.stanford.nlp.dcoref.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.DefaultPaths;
import edu.stanford.nlp.util.StringUtils;
import edu.stanford.nlp.util.logging.NewlineLogFormatter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;


public class IntermediateCoreferenceSystem extends SieveCoreferenceSystem {
    public IntermediateCoreferenceSystem(Properties props) throws Exception {
        super(props);
        List<String> lines = new ArrayList<>();
        // header
        StringJoiner header = new StringJoiner("\t");
        header.add("DOC_ID");
        header.add("PARAGRAPH_ID");
        header.add("SENT_ID");
        header.add("MENTION_ID");
        header.add("START_INDEX");
        header.add("END_INDEX");
        header.add("MENTION");
        header.add("NER_ENTITY");
        header.add("HEAD_WORD_LEMMA");
        header.add("HEAD_POS_TAG");
        header.add("COREF_ID");
        header.add("GOLD_COREF_ID");
        header.add("NER_ENTITY_TAG");
        header.add("POS_TAG_SEQ");
        lines.add(header.toString());
        try {
            Files.write(Paths.get("corefs.txt"), lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<Integer, CorefChain> coref(Document document) throws Exception {
        Map<Integer, CorefChain> corefChains = super.coref(document);
        List<String> lines = new ArrayList<>();
        List<List<Mention>> orderedMentions = filterMentionsWithSingletonClusters(document, document.predictedOrderedMentionsBySentence);
        for (List<Mention> mentionCluster: orderedMentions) {

            for (Mention mention : mentionCluster) {
                String posHeadTag = "NULL";
                String lemma;
                try {
                    lemma = mention.headIndexedWord.toString().split("/")[0];
                }
                catch (NullPointerException e) {
                    lemma = mention.headString;
                }

                try {
                    posHeadTag = mention.headIndexedWord.toString().split("/")[1];
                } catch (NullPointerException ignored) {
                }

                List<String> posSeq = new ArrayList<>();
                for (CoreLabel label: mention.originalSpan) {
                    posSeq.add(label.tag());
                }

                StringJoiner line = new StringJoiner("\t");
                line.add(document.conllDoc.getDocumentID());
                line.add(document.conllDoc.getPartNo());
                line.add(String.valueOf(mention.sentNum));
                line.add(String.valueOf(mention.mentionID));
                line.add(String.valueOf(mention.startIndex));
                line.add(String.valueOf(mention.endIndex));
                line.add(mention.spanToString());
                line.add(mention.nerName());
                line.add(lemma);
                line.add(posHeadTag);
                line.add(String.valueOf(mention.corefClusterID));
                if (document.allGoldMentions.containsKey(mention.mentionID))
                    line.add(String.valueOf(document.allGoldMentions.get(mention.mentionID).goldCorefClusterID));
                else
                    line.add(String.valueOf(mention.goldCorefClusterID));
                line.add(mention.nerString);
                line.add(String.join(" ", posSeq));
                lines.add(line.toString());
            }
        }
        try {
            Files.write(Paths.get("corefs.txt"), lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return corefChains;
    }

    /**
     * Returns the name of the log file that this method writes.
     */
    public static String initializeAndRunCoref(Properties props) throws Exception {
        String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-").replaceAll(":", "-");
        LogisticClassifier<String, String> singletonPredictor = getSingletonPredictorFromSerializedFile(props.getProperty(Constants.SINGLETON_MODEL_PROP, DefaultPaths.DEFAULT_DCOREF_SINGLETON_MODEL));

        //
        // initialize logger
        //
        String logFileName = props.getProperty(Constants.LOG_PROP, "log.txt");
        if (logFileName.endsWith(".txt")) {
            logFileName = logFileName.substring(0, logFileName.length() - 4) + "_" + timeStamp + ".txt";
        } else {
            logFileName = logFileName + "_" + timeStamp + ".txt";
        }
        try {
            FileHandler fh = new FileHandler(logFileName, false);
            logger.addHandler(fh);
            logger.setLevel(Level.WARNING);
            fh.setFormatter(new NewlineLogFormatter());
        } catch (SecurityException | IOException e) {
            System.err.println("ERROR: cannot initialize logger!");
            throw e;
        }

        Constants.printConstants(logger);

        // initialize coref system
        SieveCoreferenceSystem corefSystem = new IntermediateCoreferenceSystem(props);

        MentionExtractor mentionExtractor = null;
        if (props.containsKey(Constants.CONLL2011_PROP)) {
            mentionExtractor = new CoNLLMentionExtractor(corefSystem.dictionaries(), props,
                    corefSystem.semantics(), singletonPredictor);
        }
        if (mentionExtractor == null) {
            throw new RuntimeException("No input file specified!");
        }
        // Set mention finder
        String mentionFinderClass = props.getProperty(Constants.MENTION_FINDER_PROP);
        if (mentionFinderClass != null) {
            String mentionFinderPropFilename = props.getProperty(Constants.MENTION_FINDER_PROPFILE_PROP);
            CorefMentionFinder mentionFinder;
            if (mentionFinderPropFilename != null) {
                Properties mentionFinderProps = new Properties();
                FileInputStream fis = new FileInputStream(mentionFinderPropFilename);
                mentionFinderProps.load(fis);
                fis.close();
                mentionFinder = (CorefMentionFinder) Class.forName(mentionFinderClass).getConstructor(Properties.class).newInstance(mentionFinderProps);
            } else {
                mentionFinder = (CorefMentionFinder) Class.forName(mentionFinderClass).newInstance();
            }
            mentionExtractor.setMentionFinder(mentionFinder);
        }
        if (mentionExtractor.mentionFinder == null) {
            logger.warning("No mention finder specified, but not using gold mentions");
        }

        try {
            runAndScoreCoref(corefSystem, mentionExtractor, props, timeStamp);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "ERROR in running coreference", ex);
        }
        return logFileName;
    }

    public static void main(String[] args) throws Exception {
        Properties props = StringUtils.argsToProperties(args);
        initializeAndRunCoref(props);
    }
}

