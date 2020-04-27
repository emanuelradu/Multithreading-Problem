import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

/**
 * Class for a miner.
 */
public class Miner extends Thread {
	/**
	 * Creates a {@code Miner} object.
	 * 
	 * @param hashCount
	 *            number of times that a miner repeats the hash operation when
	 *            solving a puzzle.
	 * @param solved
	 *            set containing the IDs of the solved rooms
	 * @param channel
	 *            communication channel between the miners and the wizards
	 */
	Integer hashCount;
	Set<Integer> solved;
	CommunicationChannel channel;
	
	public Miner(Integer hashCount, Set<Integer> solved, CommunicationChannel channel) {
		this.hashCount = hashCount;
		this.solved = solved;
		this.channel = channel;
	}

    private String encryptMultipleTimes(String input, Integer count) {
        String hashed = input;
        for (int i = 0; i < count; ++i) {
            hashed = encryptThisString(hashed);
        }

        return hashed;
    }

    private String encryptThisString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // convert to string
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
            String hex = Integer.toHexString(0xff & messageDigest[i]);
            if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
    
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
	
	@Override
	public void run() {
		Message mod;
		while (true) {
			mod = channel.getMessageWizardChannel();
			if (mod == null) {
				break;
			}
			if (mod.getData() == Wizard.EXIT) {
				break;
			}
			if (solved.contains(mod.getCurrentRoom())) {
				continue;
			}
			int parent = mod.getParentRoom();
			if (parent == -1) {
				parent = mod.getCurrentRoom();
			}
			String data = encryptMultipleTimes(mod.getData(), hashCount);
			Message solution = new Message(parent, mod.getCurrentRoom(), data);
			channel.putMessageMinerChannel(solution);
			synchronized(channel) {
				solved.add(mod.getCurrentRoom());
			}
		}
	}
}
