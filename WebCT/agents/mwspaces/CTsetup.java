package mwspaces;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;

import tuplespace.*;

import com.gigaspaces.events.DataEventSession;
import com.gigaspaces.events.EventSessionConfig;
import com.gigaspaces.events.EventSessionFactory;
import com.j_spaces.core.IJSpace;

import edu.harvard.eecs.airg.coloredtrails.server.ServerGameStatus;
import edu.harvard.eecs.airg.coloredtrails.shared.discourse.BasicProposalDiscourseMessage;
import edu.harvard.eecs.airg.coloredtrails.shared.types.ChipSet;
import edu.harvard.eecs.airg.coloredtrails.shared.types.PlayerStatus;
import edu.harvard.eecs.airg.coloredtrails.shared.types.RowCol;

import org.apache.log4j.Logger;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.space.UrlSpaceConfigurer;
import org.springframework.dao.DataAccessException;

import net.jini.core.event.RemoteEventListener;

public class CTsetup {
	
	private GigaSpace space;
	private DataEventSession session;
	public static String[] agents = {"10", "20"};
	
	private Logger log = Logger.getRootLogger();
	private Integer clock = 0;
	private ServerGameStatus gs;
	Hashtable <Integer,PlayerStatus> ops;
	 

	public CTsetup(ServerGameStatus gs) {
		this.gs = gs;
	}

	public void initializeGS() {
		log.info("Starting spaces 1");
//	    	try {
//	    		 System.out.println("zatim dobry");
//	        	File file = new File("./log/"+ new Date(System.currentTimeMillis()) +".log");
//
//	            // Create file if it does not exist
//	            boolean success = file.createNewFile();
//	            if (success) {
//	                // File did not exist and was created
//	            } else {
//	                // File already exists
//	            }
//	            
//	            PrintStream printStream;
//	    		try {
//	    			printStream = new PrintStream(new FileOutputStream(file));
//	    			System.setOut(printStream);
//	    		} catch (FileNotFoundException e1) {
//	    			// TODO Auto-generated catch block
//	    			e1.printStackTrace();
//	    		}
//	        } catch (IOException e) {
//	        	
//	        }
	    	//IJSpace ispace = new UrlSpaceConfigurer("jini://*/*/myGrid").space();
	        // use gigaspace wrapper to for simpler API
	        //this.space = new GigaSpaceConfigurer(ispace).gigaSpace();
	        this.space=DataGridConnectionUtility.getSpace("myGrid");
	        //space.clear(null);
	        //dumpGSdata();
	        EventSessionConfig config = new EventSessionConfig();
	        config.setFifo(true);
	        //config.setBatch(100, 20);
	        IJSpace ispace = new UrlSpaceConfigurer("jini://*/*/myGrid").space();
	        EventSessionFactory factory = EventSessionFactory.getFactory(ispace);
	        try {
				session = factory.newDataEventSession(config);
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 
	        System.out.println("zatim dobry");
	        try {
				registerCT();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }

	 private void registerCT() throws RemoteException {
			
			CTHandler handler = new CTHandler(this);
			
			try {
				//session.addListener(new Position(), handler); 
				session.addListener(new Time(), handler); 
				//session.addListener(new Tile(), handler); 
				//session.addListener(new GroupCoin(), handler); 
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
	 }
	 public void createEntry (TimeEntry te) {
		 try{			
				if (te.getTime() == null)
					te.setTime();
				System.out.println("CT writes: "+te.toString());
				space.write(te);

				return;
			}catch (Exception e){ e.printStackTrace(); 
			
			return; }
	 }
	 
	 public void writeGoal(edu.harvard.eecs.airg.coloredtrails.shared.types.Goal goal2) {
		 tuplespace.Goal goal = new Goal();
		 goal.cell = new Cell(goal2.getLocation().row,goal2.getLocation().col);
		 goal.clock = clock;
		 System.out.println("CT writes goal: "+goal.toString());
		 createEntry(goal);
	 }

	public void event(TimeEntry e) {
		if (e instanceof tuplespace.Time) {
			this.clock = e.getClock();
		}
	}

	public void writeProposal(BasicProposalDiscourseMessage m) {
		String a1 = getAgent(m.getProposerID());
		String a2 =  getAgent(m.getResponderID());
		Proposal p = new Proposal(m.getMessageId(),a1,a2,clock);
		createEntry(p);	
	}

	public void writeResponse(int messageId, String string) {
		Response m = new Response(messageId, string, clock);
		createEntry(m);
	}
	
	private String getAgent(int pin) {
		return "a" + String.valueOf(gs.getPlayerByPerGameId(pin).getPin());
	}

	public void writeTile(int r, int c, String color) {
		Tile t = new Tile(new Cell(r,c),color,clock);
		createEntry(t);
	}

	public void writePlayers(Set<PlayerStatus> ps) {
		Hashtable <Integer,PlayerStatus> newold = new Hashtable<Integer, PlayerStatus>();
		while (ps.iterator().hasNext())
		{			
			PlayerStatus p = ps.iterator().next();
			newold.put(p.getPerGameId(), p);
			if (!p.hasChanged())
				continue;
			PlayerStatus op = ops.get(p.getPerGameId());
			if (!p.getPosition().equals(op.getPosition()))
				writePosition(p.getPerGameId(),p.getPosition());
			if (!p.getChips().equals(op.getChips()))
				writeChips(p.getPerGameId(),p.getChips());
		}
		ops = newold;
		
	}

	private void writeChips(int perGameId, ChipSet chips) {
		for(String color : chips.getColors()){
			Chip c = new Chip (getAgent(perGameId),color,chips.getNumChips(color),clock);
			createEntry(c);
		}		
	}

	private void writePosition(int perGameId, RowCol position) {
		Position p = new Position(getAgent(perGameId),new Cell(position.row,position.col),clock);
		createEntry(p);
	}
}
