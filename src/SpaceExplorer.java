import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.nio.charset.StandardCharsets;

/**
 * Class for a space explorer.
 */
public class SpaceExplorer extends Thread {

	/**
	 * Creates a {@code SpaceExplorer} object.
	 * 
	 * @param hashCount
	 *            number of times that a space explorer repeats the hash operation
	 *            when decoding
	 * @param discovered
	 *            set containing the IDs of the discovered solar systems
	 * @param channel
	 *            communication channel between the space explorers and the
	 *            HeadQuarters
	 */
	Integer hashCount;
	HashSet<Integer> discovered;
	CommunicationChannel channel;
	ReentrantLock myLock = new ReentrantLock();
	
	public SpaceExplorer(Integer hashCount, Set<Integer> discovered, CommunicationChannel channel) {
		this.hashCount = hashCount;
		this.discovered = (HashSet<Integer>) discovered;
		this.channel = channel;
	}

	@Override
	public void run() {
		Message message;
		Message solution;
		while (true) {
			message = channel.getMessageHeadQuarterChannel();
			int currentSolarSystem = message.getCurrentSolarSystem();
			if (message == null) {
				break;
			}
			if (message.getData() == "EXIT") {
				break;
			}
			if (discovered.contains(currentSolarSystem)) {
				continue;
			}
			String notEncryptedData = message.getData();
			String data = encryptMultipleTimes(notEncryptedData, hashCount);
			if (message.getParentSolarSystem() == -1) {
				solution = new Message(currentSolarSystem, currentSolarSystem, data);
			}
			solution = new Message(message.getParentSolarSystem(), currentSolarSystem, data);
			channel.putMessageSpaceExplorerChannel(solution);
			try {
				myLock.lock();
				discovered.add(currentSolarSystem);
			} catch(Exception e) {
				
			}
			finally {
				myLock.unlock();
			}
		}
	}
	
	/**
	 * Applies a hash function to a string for a given number of times (i.e.,
	 * decodes a frequency).
	 * 
	 * @param input
	 *            string to he hashed multiple times
	 * @param count
	 *            number of times that the string is hashed
	 * @return hashed string (i.e., decoded frequency)
	 */
	private String encryptMultipleTimes(String input, Integer count) {
		String hashed = input;
		for (int i = 0; i < count; ++i) {
			hashed = encryptThisString(hashed);
		}

		return hashed;
	}

	/**
	 * Applies a hash function to a string (to be used multiple times when decoding
	 * a frequency).
	 * 
	 * @param input
	 *            string to be hashed
	 * @return hashed string
	 */
	private static String encryptThisString(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));

			// convert to string
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				String hex = Integer.toHexString(0xff & messageDigest[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
