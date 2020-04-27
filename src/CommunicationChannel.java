import java.util.HashMap;
import java.util.concurrent.*;

/**
 * Class that implements the channel used by headquarters and space explorers to communicate.
 */
public class CommunicationChannel {
	private int cpy = 700;
	private BlockingQueue<Message> headQuarterChannel = new ArrayBlockingQueue<>(cpy);
	private BlockingQueue<Message> spaceExplorerChannel = new ArrayBlockingQueue<>(cpy);
	private ConcurrentHashMap<Long, Message> myMap = new ConcurrentHashMap<>();
	Semaphore semaphore1;
	Semaphore semaphore2;

	/**
	 * Creates a {@code CommunicationChannel} object.
	 */
	public CommunicationChannel() {
		semaphore1 = new Semaphore(1);
		semaphore2 = new Semaphore(1);
	}

	/**
	 * Puts a message on the space explorer channel (i.e., where space explorers write to and 
	 * headquarters read from).
	 * 
	 * @param message
	 *            message to be put on the channel
	 */
	public void putMessageSpaceExplorerChannel(Message message) {
		try {
			spaceExplorerChannel.put(message);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Gets a message from the space explorer channel (i.e., where space explorers write to and
	 * headquarters read from).
	 * 
	 * @return message from the space explorer channel
	 */
	public Message getMessageSpaceExplorerChannel() {
		Message message = null;
		try {
			message = spaceExplorerChannel.take();
		} catch (InterruptedException e) {
		}
		return message;
	}

	/**
	 * Puts a message on the headquarters channel (i.e., where headquarters write to and 
	 * space explorers read from).
	 * 
	 * @param message
	 *            message to be put on the channel
	 */
	public void putMessageHeadQuarterChannel(Message message) {
		final long threadId = Thread.currentThread().getId();
		Thread myThread = Thread.currentThread();
		Message myMessage = myMap.get(threadId);
		String myData = message.getData();
		
		try {
			semaphore1.acquire();
		} catch(InterruptedException e) {
			
		}
		try {
			if (myData == "EXIT") {
				headQuarterChannel.put(message);
			} else if (myData == "END") {
			} else if (myMap.putIfAbsent(threadId, message) != null) {
				headQuarterChannel.put(myMessage);
				headQuarterChannel.put(message);
				myMap.remove(threadId);
			}
		} catch (InterruptedException e) {
			
		}
		semaphore1.release();
	}

	/**
	 * Gets a message from the headquarters channel (i.e., where headquarters write to and
	 * space explorer read from).
	 * 
	 * @return message from the header quarter channel
	 */
	public Message getMessageHeadQuarterChannel() {
		Message message;
		int solarSystem1;
		int solarSystem2;
		String data2;
		while(true) {
			try {
				semaphore2.acquire();
			} catch (InterruptedException e) {
				
			}
			try {
				if (headQuarterChannel.peek() != null) {
					Message message1 = headQuarterChannel.take();
					solarSystem1 = message1.getCurrentSolarSystem();
					String data1 = message1.getData();
					if (data1 != "EXIT") {
						Message message2 = headQuarterChannel.take();
						data2 = message2.getData();
						solarSystem2 = message2.getCurrentSolarSystem();
						message = new Message(solarSystem1, solarSystem2, data2);
					} else {
						message = message1;
					}
					semaphore2.release();
					return message;
				}
			} catch (InterruptedException e) {
					
			}
			semaphore2.release();
		}
	}
}

