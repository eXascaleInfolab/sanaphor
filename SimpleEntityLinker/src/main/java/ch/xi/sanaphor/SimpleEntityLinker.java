package ch.xi.sanaphor;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by alberto on 14/11/16.
 * <p>
 * Simply links entities by looking up their mentions in a Lucene index.
 * Uses Wikipedia redirects to look for additional surface forms of entities
 * and disambiguates to exclude ambiguous entities.
 * <p>
 * This is a re-coding of what was run in the final version of the Sanaphor
 * ISWC paper.
 */
public class SimpleEntityLinker {

    private final static Logger LOGGER = Logger.getLogger(SimpleEntityLinker.class.getName());

    private final String URI_INDEX = "uriindex";
    private final String TYPE_INDEX = "typeindex";
    private final String PATH_INDEX = "pathindex";

    private IndexSearcher uriIndex, typeIndex, pathIndex;

    private Map<String, String> redirects, disambiguates;

    public SimpleEntityLinker(String trankIndexesDir,
                              String redirectsPath,
                              String disambiguatesPath) throws IOException {

        // opening indexes
        Directory d = new NIOFSDirectory(new File(trankIndexesDir + "/" + URI_INDEX));
        this.uriIndex = new IndexSearcher(DirectoryReader.open(d));

        d = new NIOFSDirectory(new File(trankIndexesDir + "/" + TYPE_INDEX));
        this.typeIndex = new IndexSearcher(DirectoryReader.open(d));

        d = new NIOFSDirectory(new File(trankIndexesDir + "/" + PATH_INDEX));
        this.pathIndex = new IndexSearcher(DirectoryReader.open(d));

        // reading redirects
        LOGGER.info("loading redirects...");
        this.redirects = //new HashMap<>();
                Files.lines(Paths.get(redirectsPath))
                .map(line -> line.trim())
                .filter(line -> !line.startsWith("#") && !line.isEmpty())
                .map(line -> line.split(" "))
                .collect(Collectors.toMap(
                        split -> split[0].substring(1, split[0].length() - 1),
                        split -> split[2].substring(1, split[2].length() - 1)));

        // reading disambiguates
        LOGGER.info("loading disambiguations...");
        this.disambiguates = //new HashMap<>();
                Files.lines(Paths.get(disambiguatesPath))
                        .map(line -> line.trim())
                        .filter(line -> !line.startsWith("#") && !line.isEmpty())
                        .map(line -> line.split(" "))
                        .collect(Collectors.toMap(
                                split -> split[0].substring(1, split[0].length() - 1),
                                split -> split[2].substring(1, split[2].length() - 1),
                                (x, y) -> {
//                            System.out.println("found duplicate disambiguation" + x);
                                    return x;
                                }));
    }//constructor


    public Optional<String[]> getTypes(String entityUri) throws IOException {
        TermQuery exactQuery = new TermQuery(new Term("uri", entityUri));
        TopDocs docs = this.typeIndex.search(exactQuery, 1);
        if (docs.scoreDocs.length > 0) {
            Document d = typeIndex.doc(docs.scoreDocs[0].doc);
            return Optional.ofNullable(d.getValues("type"));
        } else {
            return Optional.empty();
        }//if
    }//getTypes


    private Optional<Integer> getTypeDepth(String typeUri) throws IOException {
        TermQuery query = new TermQuery(new Term("uri", typeUri));
        TopDocs docs = pathIndex.search(query, 1);
        if(docs.scoreDocs.length == 0) return Optional.empty();

        Document typeInfo = pathIndex.doc(docs.scoreDocs[0].doc);
        return Optional.ofNullable(Integer.parseInt(typeInfo.get("level")));
    }//getTypeDepth


    public Optional<String> getDeepestType(String entityUri) throws IOException {
        Optional<String[]> allTypes = getTypes(entityUri);
        if(!allTypes.isPresent()) return Optional.empty();

        // this doesn't work: you cannot re-throw an exception from inside a lambda
        // Arrays.stream(allTypes.get()).map(...)
        int maxDepth = -1;
        String bestType = "";
        for( String eType : allTypes.get() ){
            Optional<Integer> depth = getTypeDepth(eType);
            if(depth.orElse(-1) > maxDepth) {
                maxDepth = depth.get();
                bestType = eType;
            }//if
            System.out.println("" + eType + ": " + depth);
        }//for

        return Optional.of(bestType);
    }//getDeepestType


    public Optional<String> linkMention(String mention) throws IOException {
        TermQuery exactQuery = new TermQuery(new Term("labelex", mention.toLowerCase()));

        TopDocs docs = this.uriIndex.search(exactQuery, 100);

        ArrayList<String> entitiesRetrieved = new ArrayList<>(100);
        for (ScoreDoc sc : docs.scoreDocs) {
            String entityUri = uriIndex.doc(sc.doc).get("uri");
            String redirectedEntity =
                    this.redirects.containsKey(entityUri) ? redirects.get(entityUri) : entityUri;
            // ignore entities which are ambiguous
            if (!this.disambiguates.containsKey(redirectedEntity))
                entitiesRetrieved.add(redirectedEntity);
        }//for
        if (entitiesRetrieved.isEmpty())
            // no candidate for current entity
            return Optional.empty();
        else
            // return always first retrieved result
            return Optional.ofNullable(entitiesRetrieved.get(0));
    }//linkMention


}//SimpleEntityLinker
