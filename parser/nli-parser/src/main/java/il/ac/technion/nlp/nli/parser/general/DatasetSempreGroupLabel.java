package il.ac.technion.nlp.nli.parser.general;

import il.ac.technion.nlp.nli.core.dataset.ExampleSplit;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public enum DatasetSempreGroupLabel {
    TRAIN("train"), DEV("dev"), TEST("test");

    public final String tag;
    DatasetSempreGroupLabel(String tag) {
        this.tag = tag;
    }

    public static DatasetSempreGroupLabel getFromSempreTag(String sempreTag) {
        for (int i=0; i<values().length; i++) {
            if (sempreTag.equals(values()[i].tag))
                return values()[i];
        }
        throw new RuntimeException("invalid sempreTag: " + sempreTag);
    }

    public ExampleSplit.SplitPart getEquivalentSplitPart() {
        switch (this) {
            case TRAIN:
                return ExampleSplit.SplitPart.TRAIN;
            case TEST:
            case DEV:
                return ExampleSplit.SplitPart.TEST;
        }
        throw new Error();
    }
}
