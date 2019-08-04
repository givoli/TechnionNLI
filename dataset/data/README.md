# Our Dataset
This directory contains the dataset that we present in the paper. 
The entire dataset is available in the EntireDataset.xml file, which can be read (as a Dataset object) using the "core" module in our code (via the XStream library).

Additionally, our dataset is available as a file tree in which each example is provided as a directory that contains three files: 
1. InitialState.xml - which holds an application state and can be read with the XStream library.
2. Utterance.txt - a simple text file that contains a single line with the utterance.
3. DesiredState.xml - which holds the application state that results from invoking the method call (that corresponds to the utterance) on the initial state, and can be read with the XStream library similarly to InitialState.xml.

The training examples are available under the "train" directory, which contains a CSV with the utterances (with their example id and domain) and a sub-directory for each domain. Each domain sub-directory contains a directory for each training example of that domain (the directory name is the example id).
The test examples are similarly available under the "test" directory.
