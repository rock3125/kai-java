package industries.vocht.viki.nnet_wsd_create;

import java.io.File;

/**
 * Created by peter on 18/12/16.
 *
 * create all the neural networks from the paths
 *
 */
public class CreateNNet {

    public static void main(String[] args) throws Exception {

        if ( args.length != 2 ) {
            System.out.println("Create neural networks for WSD");
            System.out.println("  the parsed input file is format: article per line: w1:tag1 w2:tag2 ... wn:tagn");
            System.out.println("usage: /path/to/parsed.txt /output/path/to/write/to");
            System.exit(1);
        }
        CreateNNet creator = new CreateNNet();
        // numIterations:  the number of times we run the samples through the nnets (not epochs)
        // dataItemLimit:  how many items to read at most for each nnet training set
        creator.create(args[0], args[1]);
    }


    private CreateNNet() {
    }

    /**
     * Go through the creation steps for neural networks
     */
    private void create(String trainingSetFilename, String output_directories) throws Exception {

        new File(output_directories).mkdirs();

        int windowSize = 25; // surrounding words window size
        int collectorCount = 2000; // number of items to return for top frequency matches
        long maxFileSizeInBytes = 0; // limit unlabelled files if > 0 to this many bytes
        double failThreshold = 66.0; // percentage at which labelled sets get split into good and bad

        NNetStep1 step1 = new NNetStep1();

        step1.create(trainingSetFilename, output_directories, maxFileSizeInBytes, windowSize);

        // step 1.  turn unlabelled data into labelled sets
        NNetStep2 step2 = new NNetStep2();
        step2.create(output_directories, failThreshold, collectorCount);

        // this java nnet is really bad - better to use the Keras/tensorflow LSTM one!
//        // step 2.  create the nnets from the labelled sets
//        NNetStep3 step3 = new NNetStep3();
//        step3.processAndSaveAllWords(output_directories, numIterations, dataItemLimit);
    }




}


