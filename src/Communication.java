import java.util.concurrent.Semaphore;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class that implements the channel used by wizards and miners to communicate.
 */
public class CommunicationChannel {
	private int cSize = 250;
	private ArrayBlockingQueue<Message> wizardChannel;
	private ArrayBlockingQueue<Message> minerChannel;
	private ConcurrentHashMap<Long, Message> mapPutWizardChannel = new ConcurrentHashMap<>();
	Semaphore semaphorePutWizardChannel = new Semaphore(1);
	Semaphore semaphoreGetWizardChannel = new Semaphore(1);
	
	
	
	/**
	 * Creates a {@code CommunicationChannel} object.
	 */
	public CommunicationChannel() {
		minerChannel = new ArrayBlockingQueue<>(cSize);
		wizardChannel = new ArrayBlockingQueue<>(cSize);
	}
	

	/**
	 * Puts a message on the miner channel (i.e., where miners write to and wizards
	 * read from).
	 * 
	 * @param message
	 *            message to be put on the channel
	 */
	public void putMessageMinerChannel(Message message) {
		try {
			minerChannel.put(message);
		} catch (InterruptedException e) {
			
		}
	}

	/**
	 * Gets a message from the miner channel (i.e., where miners write to and
	 * wizards read from).
	 * 
	 * @return message from the miner channel
	 */
	public Message getMessageMinerChannel() {
		try {
			return minerChannel.take();
		} catch (InterruptedException e) {
			
		}
		return null;
	}

	/**
	 * Puts a message on the wizard channel (i.e., where wizards write to and miners
	 * read from).
	 * 
	 * @param message
	 *            message to be put on the channel
	 */
	public void putMessageWizardChannel(Message message) {
		try {
			semaphorePutWizardChannel.acquire();
		} catch(InterruptedException e) {
			
		}
		try {
			if (message.getData() == Wizard.EXIT) {
				wizardChannel.put(message);
			} else if (message.getData() == Wizard.END) {
			} else if (mapPutWizardChannel.putIfAbsent(Thread.currentThread().getId(), message) != null) {
				wizardChannel.put(mapPutWizardChannel.get(Thread.currentThread().getId()));
				wizardChannel.put(message);
				mapPutWizardChannel.remove(Thread.currentThread().getId());
			}
		} catch (InterruptedException e) {
			
		}
		semaphorePutWizardChannel.release();
	}

	/**
	 * Gets a message from the wizard channel (i.e., where wizards write to and
	 * miners read from).
	 * 
	 * @return message from the miner channel
	 */
	public Message getMessageWizardChannel() {
		Message modM;
		while(true) {
			try {
				semaphoreGetWizardChannel.acquire();
			} catch (InterruptedException e) {
				
			}
			try {
				if (wizardChannel.peek() != null) {
					Message message1 = wizardChannel.take();
					if (message1.getData() != Wizard.EXIT) {
						Message message2 = wizardChannel.take();
						modM = new Message(message1.getCurrentRoom(), message2.getCurrentRoom(), message2.getData());
					} else {
						modM = message1;
					}
					semaphoreGetWizardChannel.release();
					return modM;
				}
			} catch (InterruptedException e) {
					
			}
			semaphoreGetWizardChannel.release();
		}
	}
}
