JFLAGS = -g
JC = javac

.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
		  Utility.java \
		  RemotePeerInfo.java \
		  ShutdownProcessor.java \
		  Constants.java \
		  CommonConfiguration.java \
		  ApplicationMessage.java \
		  ChokeController.java \
		  ClientLogger.java \
		  CooperativeServer.java \
		  HandshakeMessage.java \
		  MessageSender.java \
		  PeerInformation.java \
		  PeerInformationConfiguration.java \
		  OptimisticUnchokeController.java \
		  PeerController.java \
		  PeerManager.java \
		  PeerProcess.java \
		  StartRemotePeers.java
		  
default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
