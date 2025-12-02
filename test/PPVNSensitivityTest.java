import compression.grammar.RNAWithStructure;
import compression.structureprediction.*;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PPVNSensitivityTest {


    @Test
    public void testPPVNSensitivity() throws Exception {
        RNAWithStructure rRNA = new RNAWithStructure("acguuguccgg", "((((.)))..)");
        String pStructure =                                         "((()...)())";
        PPVNSensitivity pNsTest= new PPVNSensitivity(rRNA,pStructure);
        assertEquals(2, pNsTest.returnCommonPairs());
    }


    @Test
    public void testPPVNSensitivity2() throws Exception {
        RNAWithStructure rRNA = new RNAWithStructure("acguuguccggauau","((((.)))..)()()");
        String pStructure =                                            "((()...)())()..";
        PPVNSensitivity pNsTest= new PPVNSensitivity(rRNA,pStructure);
        assertEquals(3, pNsTest.returnCommonPairs());
        assertEquals(2, pNsTest.getFalsePositive());
        assertEquals(3, pNsTest.getFalseNegative());
        assertEquals(5, pNsTest.getNumberOfPairsPredicted());
        assertEquals(6, pNsTest.getNumberOfPairsReal());
    }
}
