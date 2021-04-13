/* Initial beliefs and rules */

detectedAndCanServeWith(Object) :- detected(Object)[grounded] & canServeWith(Object).
detectedAndCanServeIn(Object) :- detected(Object)[grounded] & canServeIn(Object).

isAbleToServe :- detectedAndCanServeWith(ObjectToServeWith) & detectedAndCanServeIn(ObjectToServeIn).

canServeWith(kettle).
canServeIn(coffee_cup).
		   
/* Initial goals */

!start.

/* Plans */

+!start <- .print("Hello, just give me a kettle and a coffee cup and I'll pour you some coffee.")
		   !refreshPerceptions.

+canServeWith(Object).
-canServeWith(Object).

+canServeIn(Object).
-canServeIn(Object).

+detected(woman) <- +detected(person).
+detected(man) <- +detected(person).

-detected(woman) <- -detected(person).
-detected(man) <- -detected(person).

+detected(person) <- !serve.
-detected(person) <- .remove_plan(serve).

+detected(Object) : not canServeWith(Object) & not canServeIn(Object) & not (Object == coffee) <- askTheUtility(Object).
+detected(Object) : detected(person) <- !serve.

+!updatePerceptions : not serving <- .print("generating event to refreshPercetions (now +10 s)");
					   .at("now +10 s", {+!refreshPerceptions}).

+!refreshPerceptions : not serving <- .print("Agent will refreshPerceptions (external action)...");
									  refreshPerceptions;
					    			  !updatePerceptions.

+!refreshPerceptions : serving.

+!serve : isAbleToServe & detected(person) <- .findall(O, detectedAndCanServeWith(O), L1)
							  				  .nth(0, L1, ObjToServeWith)
						      				  .findall(O, detectedAndCanServeIn(O), L2)
			                   				  .nth(0, L2, ObjToServeIn)
			                  				  .print("It will serve with ", ObjToServeWith, " in ", ObjToServeIn)
							  				  !serveWith(ObjToServeWith, ObjToServeIn).

+!serve : not detected(person).

+!serve : detectedAndCanServeIn(Object) <- requestObjectToServeWith.

+!serve : detected(Object) & canServeIn(Object) <- .print("Agent has detected(", Object,") to serve in, but it's not grounded. Therefore, it will not be considered valid to be used.").

+!serve : detectedAndCanServeWith(Object) <- requestObjectToServeIn.

+!serve : detected(Object) & canServeWith(Object) <- .print("Agent has detected(", Object,") to serve with, but it's not grounded. Therefore, it will not be considered valid to be used.").

+!serve <- .print("Waiting for the objects to serve the client with.").

+!serveWith(ObjToServeWith, ObjToServeIn) : not serving
										  <- +serving;
                                             .print("Serving the client...");
									         .print("Using: ", ObjToServeWith);
										     .print("To fill: ", ObjToServeIn);
										     serveClient(ObjToServeWith, ObjToServeIn);
										     .print("generating event o check if was successful... (now +30 s)");
										     .at("now +30 s", {+!refreshPerceptionsAndCheckSuccessful}).

+!serveWith(ObjToServeWith, ObjToServeIn) : serving.

+!refreshPerceptionsAndCheckSuccessful <- .print("The agent will verify that it was successful...");
										  verifyThatWasSuccessful;
										  !checkSuccessful;
										  -serving;
										  !updatePerceptions.

+!checkSuccessful : detected(coffee) <- .print("The client was served successfully.").

+!checkSuccessful : not detected(coffee) <- .print("Oops! There was some problem when serving the client.").

