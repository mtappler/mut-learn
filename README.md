# mut-learn

## About

This project contains the implementation of an equivalence testing approach for active automata learning.
It is implemented as an equivalence oracle for the automata-learning library [LearnLib](http://learnlib.de/).
Basically, test-cases are generated with one of the implemented random test-case generator and then 
selected with one of the selectors. The project is motivated by mutation testing (hence the name) which
provides a good selection strategy. In particular, the newly developed split-state operator represents 
a well-suited choice for automata learning. Test-case generation with respect to the change-output operator on the other
hand ensures coverage of the learned model.

## Structure 
The project is split into three parts:
* core: contains the implementation of the main functionality. 
* eval: contains some utility class for evaluation as well as some classes for performing the actual evaluation. These classes demonstrate how to use the code.
* suls: contains utility classes for loading systems from previously learned Graphviz dot-files

## Usage
The project was developed using Maven, thus it can be loaded and evaluations can be run with little effort. 
I am not a Maven-expert, though, so there may be better ways to structure the project.
It is a prototypical implementation, thus it poses some restrictions. For instance, systems to be learned need to 
use the Symbol class provided by LearnLib for inputs and strings as outputs.  

Generally, Maven should take care of most of the external dependencies but if you do not want to use Maven, the dependencies of the project are:
* Java 8
* LearnLib 0.12 (multiple libraries)
* Apache Commons Lang 3.4 

One test-case generator (in the class `AdaptedLeeYannakakisGenerator`), however, has an external dependency to the Yannakakis test-case generator implemented by Joshua Moerman
which can be found at https://gitlab.science.ru.nl/moerman/Yannakakis. So if you want to use this test-case generator you have download the C++ code from the external repository
and build it.

As noted above, Graphviz dot-files can be loaded as SULs. However, they should be given in the syntax used by LearnLib 0.12.
Perl scripts for translating the [TCP-models](http://www.sws.cs.ru.nl/publications/papers/fvaan/FJV16/) learned by Fiterau-Brostean et al. 
and the [TLS-models](http://www.cs.ru.nl/J.deRuiter/download/usenix15.zip) learned by de Ruiter and Poll can be 
found in the resources of the eval-subproject. 
Thus, in order to start some of the evaluation programs you need to:
* download the TCP/TLS-model files
* translate them via the provided Perl scripts  
* ensure that they are correctly named and 
* stored in the corresponding subdirectory of resources. 

Models of MQTT brokers learned in previous work are available in the resources directory of the eval-subproject and can be 
used as is. We will provide more information on learning MQTT broker models in our talk "Model-Based Testing IoT Communication via Active Automata Learning" 
at [ICST 2017](http://aster.or.jp/conference/icst2017/program/accepted.html).
Consequently, evaluation programs of MQTT brokers can be started right away (for convenience calls to the Yannakakis test-case generator have been commented out).
