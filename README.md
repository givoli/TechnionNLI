# Zero-Shot Semantic Parsing for Instructions

In this work we consider a zero-shot semantic parsing task: parsing instructions into compositional logical forms, in domains that were not seen during training. We present a new dataset for this task.

![](docs/images/task.jpg?raw=true "The Task")

A short description of each module follows (see `docs/Javadoc` for details).

## The Core Module
The core logic for our task is available under the `core` directory. This module provides a friendly interface for experimenting with new parsers for our task, and also allows to easily define new domains and collect for them data. Both the parser and dataset applications module (described next) depend on the core module.

For a quick glance at the core logic, check out the following test method:
`il.ac.technion.nlp.nli.core.state.StateTest#testMethodCallInvocation`


## The Dataset
Our dataset contains 7 domains and can be found in the `dataset/data` directory. The code for the applications that corresponds to these domains is under the `dataset/app-code` directory. This code also serves as a demonstration for how can one define new domains and collect for them data by generating random state pairs and creating a visualization for each pair (via html generation).

We collected the dataset by presenting human annotators with visualizations of initial and desired state pairs. The annotators were then asked to write an English instruction that can be executed in order to transfer the application from the initial state to the desired state. We collected 1,390 examples from the following 7 domains:

**CALENDAR**: removing calendar events and setting their color.

**CONTAINER**: loading, unloading and removing shipping containers.

**FILE**: removing files and moving them from one directory to another.

**LIGHTING**: turning lights on and off in rooms inside a house.

**LIST**: removing elements (integers) and moving an element to the beginning/end of a list.

**MESSENGER**: creating/deleting chat groups and muting/unmuting them. 

**WORKFORCE**: assigning employees to a new manager, firing employees, assigning an employee to a new position and updating an employee's salary.

## The Parser Module
The parser module, available under the `parser` directory, contains the implementation for the parsers that we discuss in the paper. Under this directory there is the subdirectory "modified-sempre" which contains the modified code of SEMPRE 2.1.
The subdirectory "resources" contains the grammar (derivation rules) and files used for setting up SEMPRE.

## Collecting New Datasets
If you intend to use our framework to collect a new dataset, you should download the [wkhtmltopdf]( https://wkhtmltopdf.org/) command line tool, which our framework uses to create the [initial state, desired state] images that are provided to Turkers.


Our framework is designed to allow to easily collect a new dataset, by defining new domains and collecting for them examples. In the example shown here (with the LIGHTING domain) the additional code that is required for integrating existing Java code with our framework is marked with an underline.

![](docs/images/extending_the_dataset.jpg?raw=true "Extending the Dataset")

Also, see the `dataset\app-code\resources\MTurk` directory for useful resources.

## Building the Project

You can build the project with Maven:
```
mvn compile
```


## Citation
If you use this code/data for academic work, please cite:
```
@inproceedings{givoli2019zero,
  author={Ofer Givoli and Roi Reichart}
  title={Zero-Shot Semantic Parsing for Instructions},
  booktitle={ACL},
  year={2019}
}
```

