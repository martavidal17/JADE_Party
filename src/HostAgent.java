


// Imports
///////////////
import jade.core.AID;
import jade.core.Agent;
import jade.core.ProfileImpl;
import jade.core.Profile;

import jade.wrapper.PlatformController;
import jade.wrapper.AgentController;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.DFService;
import jade.domain.FIPAException;

import javax.swing.*;
import java.util.*;
import java.text.NumberFormat;


public class HostAgent
        extends Agent
{
    // Constants
    //////////////////////////////////

    public final static String HELLO = "HELLO";
    public final static String ANSWER = "ANSWER";
    public final static String THANKS = "THANKS";
    public final static String GOODBYE = "GOODBYE";
    public final static String INTRODUCE = "INTRODUCE";

    public final static String RUMOUR = "RUMOUR";
    public final static String RUMOUR2 = "RUMOUR2";
    public final static String RUMOUR3 = "RUMOUR3";


    // Static variables
    //////////////////////////////////


    // Instance variables
    //////////////////////////////////
    protected JFrame m_frame = null;
    protected Vector m_guestList = new Vector();    // invitees
    protected int m_guestCount = 0;                 // arrivals
    protected int m_rumourCount = 0;
    protected int m_introductionCount = 0;
    protected boolean m_partyOver = false;
    protected NumberFormat m_avgFormat = NumberFormat.getInstance();
    protected long m_startTime = 0L;


    // Constructors
    //////////////////////////////////

    /**
     * Construct the host agent.  Some tweaking of the UI parameters.
     */
    public HostAgent() {
        m_avgFormat.setMaximumFractionDigits( 2 );
        m_avgFormat.setMinimumFractionDigits( 2 );
    }



    // External signature methods
    //////////////////////////////////

    /**
     * Setup the agent.  Registers with the DF, and adds a behaviour to
     * process incoming messages.
     */
    protected void setup() {
        try {
            System.out.println( getLocalName() + " setting up");

            // create the agent descrption of itself
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName( getAID() );
            DFService.register( this, dfd );

            // add the GUI
            setupUI();

            // add a Behaviour to handle messages from guests
            addBehaviour( new CyclicBehaviour( this ) {
                public void action() {
                    ACLMessage msg = receive();

                    if (msg != null) {
                        if (HELLO.equals( msg.getContent() )) {
                            // a guest has arrived
                            m_guestCount++;
                            setPartyState( "Inviting guests (" + m_guestCount + " have arrived)" );

                            if (m_guestCount == m_guestList.size()) {
                                System.out.println( "All guests have arrived, starting conversation" );
                                // all guests have arrived
                                beginConversation();
                            }
                        }
                        else if (RUMOUR.equals( msg.getContent() )) {
                            // count the agents who have heard the rumour
                            incrementRumourCount();
                        }
                        else if (msg.getPerformative() == ACLMessage.REQUEST  &&  INTRODUCE.equals( msg.getContent() )) {
                            // an agent has requested an introduction
                            doIntroduction( msg.getSender() );
                        }
                    }
                    else {
                        // if no message is arrived, block the behaviour
                        block();
                    }
                }
            } );
        }
        catch (Exception e) {
            System.out.println( "Saw exception in HostAgent: " + e );
            e.printStackTrace();
        }

    }


    // Internal implementation methods
    //////////////////////////////////

    /**
     * Setup the UI, which means creating and showing the main frame.
     */
    private void setupUI() {
        m_frame = new HostUIFrame( this );

        m_frame.setSize( 400, 200 );
        m_frame.setLocation( 400, 400 );
        m_frame.setVisible( true );
        m_frame.validate();
    }


    /**
     * Invite a number of guests, as determined by the given parameter.  Clears old
     * state variables, then creates N guest agents.  A list of the agents is maintained,
     * so that the host can tell them all to leave at the end of the party.
     *
     * @param nGuests The number of guest agents to invite.
     */
    protected void inviteGuests( int nGuests ) {
        // remove any old state
        m_guestList.clear();
        m_guestCount = 0;
        m_rumourCount = 0;
        m_introductionCount = 0;
        m_partyOver = false;
        ((HostUIFrame) m_frame).lbl_numIntroductions.setText( "0" );
        ((HostUIFrame) m_frame).prog_rumourCount.setValue( 0 );
        ((HostUIFrame) m_frame).lbl_rumourAvg.setText( "0.0" );

        // notice the start time
        m_startTime = System.currentTimeMillis();

        setPartyState( "Inviting guests" );

        PlatformController container = getContainerController(); // get a container controller for creating new agents
        // create N guest agents
        try {
            for (int i = 0;  i < nGuests;  i++) {
                // create a new agent
                String localName = "guest_"+i;
                AgentController guest = container.createNewAgent(localName, "GuestAgent", null);
                guest.start();
                //Agent guest = new GuestAgent();
                //guest.doStart( "guest_" + i );

                // keep the guest's ID on a local list
                m_guestList.add( new AID(localName, AID.ISLOCALNAME) );
            }
        }
        catch (Exception e) {
            System.err.println( "Exception while adding guests: " + e );
            e.printStackTrace();
        }
    }


    /**
     * End the party: set the state variables, and tell all the guests to leave.
     */
    protected void endParty() {
        setPartyState( "Party over" );
        m_partyOver = true;

        // log the duration of the run
        System.out.println( "Simulation run complete. NGuests = " + m_guestCount + ", time taken = " +
                m_avgFormat.format( ((double) System.currentTimeMillis() - m_startTime) / 1000.0 ) + "s" );

        // send a message to all guests to tell them to leave
        for (Iterator i = m_guestList.iterator();  i.hasNext();  ) {
            ACLMessage msg = new ACLMessage( ACLMessage.INFORM );
            msg.setContent( GOODBYE );

            msg.addReceiver( (AID) i.next() );

            send(msg);
        }

        m_guestList.clear();
    }


    /**
     * Shut down the host agent, including removing the UI and deregistering
     * from the DF.
     */
    protected void terminateHost() {
        try {
            if (!m_guestList.isEmpty()) {
                endParty();
            }

            DFService.deregister( this );
            m_frame.dispose();
            doDelete();
        }
        catch (Exception e) {
            System.err.println( "Saw FIPAException while terminating: " + e );
            e.printStackTrace();
        }
    }


    /**
     * Start the conversation in the party.  Tell a random guest a rumour, and
     * select two random guests and introduce them to each other.
     */
    protected void beginConversation() {
        // start a rumour
        ACLMessage rumour = new ACLMessage( ACLMessage.INFORM );
        rumour.setContent( RUMOUR );
        rumour.addReceiver( randomGuest( null ) );
        send( rumour );

        // introduce two agents to each other
        doIntroduction( randomGuest( null ) );
        setPartyState( "Swinging" );
    }


    /**
     * Introduce guest0 to a random other guest.  Also updates the introduction
     * count on the UI, and the avg no of introductions per rumour.
     */
    protected void doIntroduction( AID guest0 ) {
        if (!m_partyOver) {
            AID guest1 = randomGuest( guest0 );

            // introduce two guests to each other
            ACLMessage m = new ACLMessage( ACLMessage.INFORM );
            m.setContent( INTRODUCE + " " + guest0.getName() );
            m.addReceiver( guest1 );
            send( m );

            // update the count of introductions on the UI
            m_introductionCount++;
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    ((HostUIFrame) m_frame).lbl_numIntroductions.setText( Integer.toString( m_introductionCount ));
                }
            } );
            updateRumourAvg();
        }
    }


    /**
     * Increment the number of guests that have heard the rumour, and update the UI.
     * If all guests have heard the rumour, end the party.
     */
    protected void incrementRumourCount() {
        m_rumourCount++;
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                ((HostUIFrame) m_frame).prog_rumourCount.setValue( Math.round( 100 * m_rumourCount / m_guestCount ) );
            }
        } );
        updateRumourAvg();

        // when all the guests have heard the rumour, the party ends
        if (m_rumourCount == m_guestCount) {
            // simulate the user clicking stop when the guests have all heard the rumour
            try {
                SwingUtilities.invokeAndWait( new Runnable() {
                    public void run() {
                        ((HostUIFrame) m_frame).btn_stop_actionPerformed( null );
                    }
                } );
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Update the state of the party in the UI
     */
    protected void setPartyState( final String state ) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                ((HostUIFrame) m_frame).lbl_partyState.setText( state );
            }
        } );
    }


    /**
     * Update the average number of introductions per rumour spread
     * in the UI.
     */
    protected void updateRumourAvg() {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                ((HostUIFrame) m_frame).lbl_rumourAvg.setText( m_avgFormat.format( ((double) m_introductionCount) / m_rumourCount ) );
            }
        } );
    }


    /**
     * Pick a guest at random who is not the given guest.
     *
     * @param aGuest A guest at the party or null
     * @return A random guest who is not aGuest.
     */
    protected AID randomGuest( AID aGuest ) {
        AID g = null;

        do {
            int i = (int) Math.round( Math.random() * (m_guestList.size() - 1) );
            g = (AID) m_guestList.get( i );
        } while ((g!=null) && g.equals(aGuest));

        return g;
    }



    //==============================================================================
    // Inner class definitions
    //==============================================================================

}