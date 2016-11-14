package ch.xi.sanaphor;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by alberto on 14/11/16.
 */
public class SimpleEntityLinkerDemo {
    public static void main(String[] args) {

        String trankIndexes = "/Users/alberto/Documents/Projects/Sanaphor_pp/Anaphora/trank-indexes/";
        String disambiguates = "/Users/alberto/Documents/Projects/Sanaphor_pp/Anaphora/dataset/dbpedia/disambiguations_en.nt";
        String redirects = "/Users/alberto/Documents/Projects/Sanaphor_pp/Anaphora/dataset/dbpedia/redirects_transitive_en.nt";

        try {
            SimpleEntityLinker sel = new SimpleEntityLinker(trankIndexes, redirects, disambiguates);

            //links a mention
            Optional<String> entityOpt = sel.linkMention("Donald Trump");
            System.out.println(entityOpt.orElse("NO ENTITY"));
            if(entityOpt.isPresent()) {
                String entity = entityOpt.get();
                // gets all types
                Optional<String[]> typesOpt = sel.getTypes(entity);
                for(String t : typesOpt.orElse(new String[0])) {
                    System.out.println(t);
                }
                // gets the deepest type
                System.out.println("deepest type: " + sel.getDeepestType(entity));
            }//if
        } catch (IOException e) {
            e.printStackTrace();
        }

    }//main
}//SimpleEntityLinkerDemo
