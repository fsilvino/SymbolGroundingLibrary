/* Initial beliefs and rules */
sabeSeEhCortavelOuNao(Objeto) :- ehCortavel(Objeto) | naoEhCortavel(Objeto).
sabeSeEhCortanteOuNao(Objeto) :- ehCortante(Objeto) | naoEhCortante(Objeto).
sabeSeEhCortavelPorOuNao(ObjCortavel, ObjCortante) :- ehCortavelPor(ObjCortavel, ObjCortante) |
													  naoEhCortavelPor(objCortavel, ObjCortante).

ehDesconhecido(Objeto) :- not sabeSeEhCortavelOuNao(Objeto) &
                          not sabeSeEhCortanteOuNao(Objeto).
possuiObjetoCortante(Objeto) :- possui(Objeto) & ehCortante(Objeto).
possuiObjetoCortavel(Objeto) :- possui(Objeto) & ehCortavel(Objeto).

podeCortar(ObjetoCortavel, ObjetoCortante) :- possuiObjetoCortante(ObjetoCortante) &
											  possuiObjetoCortavel(ObjetoCortavel) &
											  ehCortavelPor(ObjetoCortavel, ObjetoCortante).

/* Initial goals */
!cortarObjetos.

/* Plans */
+possui(Objeto) : ehDesconhecido(Objeto) <- perguntarSeEhCortavel(Objeto)
											perguntarSeEhCortante(Objeto).

+possui(Objeto) : not sabeSeEhCortavelOuNao(Objeto) <- perguntarSeEhCortavel(Objeto).

+possui(Objeto) : not sabeSeEhCortanteOuNao(Objeto) <- perguntarSeEhCortante(Objeto).

+ehCortavel(Objeto) <- !cortarObjetos.
						
+naoEhCortavel(Objeto).

+ehCortante(Objeto) <- !cortarObjetos.
                       
+naoEhCortante(Objeto).

+ehCortavelPor(ObjetoCortavel, ObjetoCortante).

+naoEhCortavelPor(ObjCortavel, ObjetoCortante).

+!cortar(ObjetoCortavel, ObjetoCortante) : podeCortar(ObjetoCortavel, ObjetoCortante)
											<- cortar(ObjetoCortavel, ObjetoCortante)
											   .abolish(possui(ObjetoCortavel)).

-!cortar(ObjetoCortavel, ObjetoCortante) : not possui(ObjetoCortavel) <- solicitarObjeto(ObjetoCortavel).

-!cortar(ObjetoCortavel, ObjetoCortante) : not possui(ObjetoCortante) <- solicitarObjeto(ObjetoCortante).

-!cortar(ObjetoCortavel, ObjetoCortante) : naoEhCortavelPor(ObjetoCortavel, ObjetoCortante) 
											<- solicitarObjetoCortante(ObjetoCortavel, ObjetoCortante).

-!cortar(ObjetoCortavel, ObjetoCortante) : not sabeSeEhCortavelPorOuNao(ObjetoCortavel, ObjetoCortante)
											<- perguntarSeEhCortavelPor(ObjetoCortavel, ObjetoCortante). 

+!cortarObjetos <- .findall(O, possuiObjetoCortante(O), L1)
				   for (.member(ObjCortante, L1)) {
				       .findall(O, possuiObjetoCortavel(O), L2)
	                   for (.member(ObjCortavel, L2)) {
	                   	   !cortar(ObjCortavel, ObjCortante)
	                   }
				   }.
				   
				   