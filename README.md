# mut-learn

Content will soon be available ...

## About

This project contains the implementation of an equivalence testing approach for active automata learning.
It is implemented as an equivalence oracle for the automata-learning library [LearnLib](http://learnlib.de/).
Basically, test-cases are generated with one of the implemented random test-case generator and then 
selected with one of the selectors. The project is motivated by mutation testing (hence the name) which
provides a good selection strategy. In particular, the newly developed split-state operator represents 
a well-suited choice for automata learning. Test-case generation with change-output operator on the other
hand ensures coverage of the learned model.

## Structure 
The project is split into three parts:
* core: contains the implementation of the main functionality. 
* eval: contains some utility class for evaluation as well as some classes for performing the actual evaluation. These classes demonstrate how to use the code.
* suls: utility class for loading systems from previously learned Graphviz dot-files and another benchmark system.

The project was developed using Maven, thus can with little effort be loaded and evaluations can be run. 
I am not a Maven-expert, though, so there may be better ways to structure the project.
It is a prototypical implementation, thus it poses some restrictions. For instance, systems to be learned need to use
use the Symbol class provided by LearnLib for inputs and strings as outputs.  

As noted above, Graphviz dot-files can be loaded as SULs. However, they should be given in the syntax used by LearnLib 0.12.
Script for translating the [TCP-models](http://www.sws.cs.ru.nl/publications/papers/fvaan/FJV16/) learned by Fiterau-Brostean et al. 
and the [TLS-models](http://www.cs.ru.nl/J.deRuiter/download/usenix15.zip) learned by de Ruiter and Poll can be 
found in the resources of the eval-subproject. 