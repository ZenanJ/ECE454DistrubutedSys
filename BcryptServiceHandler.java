import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.mindrot.jbcrypt.BCrypt;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TTransportFactory;

public class BcryptServiceHandler implements BcryptService.Iface {

	//private static List<Worker> workerPool = new ArrayList<Worker>();
	private static List<Worker> workerPool;
	private Lock socketLock = new ReentrantLock();
    static {
        workerPool = new ArrayList<>();
        Worker defaultWorker = new Worker("",0, true); // Create a default Worker object
        workerPool.add(defaultWorker); // Add the default Worker to the workerPool list
    }
	private static final int numThread = Runtime.getRuntime().availableProcessors();
	//private static boolean isInitalFE = false;
	
	public List<String> hashPassword(List<String> password, short logRounds) throws IllegalArgument, org.apache.thrift.TException
	{
		try {
			if (password.isEmpty()) {
				throw new IllegalArgument("Password list is empty!");
			}
			if(logRounds > 16 || logRounds < 4){
				throw new IllegalArgument("LogRounds Out of range");
			}
			//add FE own woker
			
			// if(!isInitalFE){
			// 	System.out.println("add frontend worker");
			// 	workerPool.add(new Worker(null));
			// 	isInitalFE = true;
			// }
			//1.check if there is any BE connected
			List<String> hash = assignHashPassword(password, logRounds);
			return hash;
		} catch (Exception e) {
			throw new IllegalArgument(e.getMessage());
		}
		
	}

	private List<String> assignHashPassword(List<String> password, short logRounds) throws IllegalArgument, org.apache.thrift.TException
	{
		try {

			if(password.size() == 1){
				Worker currentWorker = workerPool.get(0);
				for(int i = 0; i < workerPool.size(); i++){
					if(workerPool.get(i).getWorkload()< currentWorker.getWorkload()){
						currentWorker = workerPool.get(i);
					}
				}
				//System.out.println("currentworker load is " + currentWorker.getWorkload());
				int tempWorkload = workloadCal(password, logRounds);
				currentWorker.addWorkload(tempWorkload);
				List<String> tempres = new ArrayList<String>();
				if(currentWorker.isFrontend() == true){
					try {
						tempres = this.hashPasswordFunction(password, logRounds);
					} catch (TException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else{
					try {
						tempres = callBEHashPassword(currentWorker.getWorkerClient(), password, logRounds);
					} catch (TException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				currentWorker.removeWorkload(tempWorkload);
				return tempres;
			}
			else{
				//System.out.println("get another connection!!!!!!!!!!!!!!!!!!!!!!!!");
				int currentWorkload = workloadCal(password, logRounds);
				int totalWorker = workerPool.size();
				int pointer = 0;
				
				String[] res = new String[password.size()];
				List<Integer>head = new ArrayList<Integer>();
				List<Integer>tail = new ArrayList<Integer>();
				for(int x = 0; x < workerPool.size(); x++){
					Worker currentWorker = workerPool.get(x);
					
					if(currentWorker.getWorkload() < currentWorkload || x == workerPool.size() - 1){
						//System.out.println("in if: currentworker is " + x + "   hisload is " + currentWorker.getWorkload()+ "    currentload is " + currentWorkload);
						
						if(x != workerPool.size() - 1){
							
							int length = (password.size()-pointer)/(totalWorker - x);
							if(length == 0 && pointer < password.size()){
								length = 1;
							}
							head.add(Integer.valueOf(pointer));
							tail.add(Integer.valueOf(pointer+ length));
							//System.out.println("if worker "+x+" get load: "+ currentWorker.getWorkload()  + " head: " + head + " tail: " + tail);
							pointer = tail.get(x);
						}
						else{
							head.add(Integer.valueOf(pointer));
							tail.add(Integer.valueOf(password.size()));
							pointer = password.size();
							//System.out.println("else worker "+x+" get load"+ currentWorker.getWorkload() + " head: " + head + " tail: " + tail);
						}
						
					}
					else{
						head.add(Integer.valueOf(pointer));
						tail.add(Integer.valueOf(pointer));
					}
				}

				//new thread
				
				//create multi thread
				ExecutorService executorService = Executors.newFixedThreadPool(Math.min(workerPool.size(), password.size()));
					
					for(int x = 0; x < workerPool.size(); x++){
						final Worker currentWorker = workerPool.get(x);
						final Integer tempHead = head.get(x);
						final Integer tempTail = tail.get(x);
						final List<String> splitPassword = password.subList(tempHead,tempTail);
						//System.out.println("currentworker with its splitP SIZE: " + x + " : " + splitPassword.size() + " temphead " + tempHead + " temptail " + tempTail);
						final int tempWorkload = workloadCal(splitPassword, logRounds);
						workerPool.get(x).addWorkload(tempWorkload);
						//System.out.println("worker: " + x + "   load " + splitPassword.size());
						if(splitPassword.size() != 0){
							executorService.submit(() -> {
								List<String> tempres = new ArrayList<String>();
								if(currentWorker.isFrontend() == true){
									try {
										tempres = this.hashPasswordFunction(splitPassword, logRounds);
									} catch (TException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								else{
									try {
										tempres = callBEHashPassword(currentWorker.getWorkerClient(), splitPassword, logRounds);
									} catch (TException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								currentWorker.removeWorkload(tempWorkload);
								
								String[] array = tempres.toArray(new String[0]);
								
								for(int y = tempHead; y < tempTail; y++){
									res[y] = array[y-tempHead];
								}

							});
						}
					}
				
				executorService.shutdown();
				executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
				
				return  Arrays.asList(res);
			}
			
			
		} catch (Exception e) {
			throw new IllegalArgument(e.getMessage());
		}
		
	}
	private List<String> callBEHashPassword(BcryptService.Client client, List<String> password, short logRounds) throws IllegalArgument, org.apache.thrift.TException{
		try {
			client.getInputProtocol().getTransport().open();
			List<String> hash = client.hashPasswordFunction(password, logRounds);
			client.getInputProtocol().getTransport().close();
			return hash;
		} catch (Exception e) {
			throw new IllegalArgument(e.getMessage());
		}
		
	}

	public List<Boolean> checkPassword(List<String> password, List<String> hash) throws IllegalArgument, org.apache.thrift.TException
	{
		try {
			//multiple password
			if (password.size() != hash.size()) {
				throw new IllegalArgument("Password and Hash size is not matching!");
			}
			for(String childHash: hash){
				if(isBcryptHashMalformed(childHash)){
					throw new IllegalArgument("malformed hash passed to checkPassword!");
				}
			}
			// if(!isInitalFE){
			// 	workerPool.add(new Worker(null));
			// 	isInitalFE = true;
			// }
			
			List<Boolean> ret = assignCheckPassword(password, hash);
			
			return ret;

		} catch (Exception e) {
			throw new IllegalArgument(e.getMessage());
		}
	}
	private List<Boolean> assignCheckPassword(List<String> password, List<String> hash) throws IllegalArgument, org.apache.thrift.TException
	{
		try {
			if(password.size() == 1){
				Worker currentWorker = workerPool.get(0);
				for(int i = 0; i < workerPool.size(); i++){
					if(workerPool.get(i).getCheckload()< currentWorker.getCheckload()){
						currentWorker = workerPool.get(i);
					}
				}
				//System.out.println("currentworker load is " + currentWorker.getWorkload());
				
				currentWorker.addCheckload(1);
				List<Boolean> tempres = new ArrayList<Boolean>();
				if(currentWorker.isFrontend() == true){
					try {
						tempres = this.checkPasswordFunction(password, hash);
					} catch (TException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else{
					try {
						tempres = callBECheckPassword(currentWorker.getWorkerClient(), password, hash);
					} catch (TException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				currentWorker.removeCheckload(1);
				return tempres;
			}
			else{
				int currentWorkload = password.size();
				int totalWorker = workerPool.size();
				int pointer = 0;
				
				Boolean[] res = new Boolean[password.size()];
				List<Integer>head = new ArrayList<Integer>();
				List<Integer>tail = new ArrayList<Integer>();
				for(int x = 0; x < workerPool.size(); x++){
					Worker currentWorker = workerPool.get(x);
					
					if(currentWorker.getCheckload() < currentWorkload || x == workerPool.size() - 1){
						//System.out.println("in if: currentworker is " + x + "   hisload is " + currentWorker.getWorkload()+ "    currentload is " + currentWorkload);
						
						if(x != workerPool.size() - 1){
							
							int length = (password.size()-pointer)/(totalWorker - x);
							if(length == 0 && pointer < password.size()){
								length = 1;
							}
							head.add(Integer.valueOf(pointer));
							tail.add(Integer.valueOf(pointer+ length));
							//System.out.println("if worker "+x+" get load: "+ currentWorker.getWorkload()  + " head: " + head + " tail: " + tail);
							pointer = tail.get(x);
						}
						else{
							head.add(Integer.valueOf(pointer));
							tail.add(Integer.valueOf(password.size()));
							pointer = password.size();
							//System.out.println("else worker "+x+" get load"+ currentWorker.getWorkload() + " head: " + head + " tail: " + tail);
						}
						
					}
					else{
						head.add(Integer.valueOf(pointer));
						tail.add(Integer.valueOf(pointer));
					}
				}

				//new thread
				
				//create multi thread
				ExecutorService executorService = Executors.newFixedThreadPool(Math.min(workerPool.size(), password.size()));
					
					for(int x = 0; x < workerPool.size(); x++){
						final Worker currentWorker = workerPool.get(x);
						final Integer tempHead = head.get(x);
						final Integer tempTail = tail.get(x);
						final List<String> splitPassword = password.subList(tempHead,tempTail);
						final List<String> splitHash = hash.subList(tempHead,tempTail);
						//System.out.println("currentworker with its splitP SIZE: " + x + " : " + splitPassword.size() + " temphead " + tempHead + " temptail " + tempTail);
						final int tempWorkload = splitPassword.size();
						workerPool.get(x).addCheckload(tempWorkload);
						//System.out.println("worker: " + x + "   load " + splitPassword.size());
						if(splitPassword.size() != 0){
							executorService.submit(() -> {
								List<Boolean> tempres = new ArrayList<Boolean>();
								if(currentWorker.isFrontend() == true){
									try {
										tempres = this.checkPasswordFunction(splitPassword, splitHash);
									} catch (TException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								else{
									try {
										tempres = callBECheckPassword(currentWorker.getWorkerClient(), splitPassword, splitHash);
									} catch (TException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								currentWorker.removeCheckload(tempWorkload);
								
								Boolean[] array = tempres.toArray(new Boolean[0]);
								
								for(int y = tempHead; y < tempTail; y++){
									res[y] = array[y-tempHead];
								}

							});
						}
					}
				
				executorService.shutdown();
				executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
				
				return  Arrays.asList(res);
			}
			
			
		} catch (Exception e) {
			throw new IllegalArgument(e.getMessage());
		}
	}

	private List<Boolean> callBECheckPassword(BcryptService.Client client, List<String> password, List<String> hash) throws IllegalArgument, org.apache.thrift.TException{
		try {
			if(!client.getInputProtocol().getTransport().isOpen()){
				client.getInputProtocol().getTransport().open();
			}
			//client.getInputProtocol().getTransport().open();
			List<Boolean> res = client.checkPasswordFunction(password, hash);
			//client.getInputProtocol().getTransport().close();
			return res;
		} catch (Exception e) {
			throw new IllegalArgument(e.getMessage());
		}
		
	}

	
	public List<String> hashPasswordFunction(List<String> password, short logRounds) throws IllegalArgument, org.apache.thrift.TException
	{
		try {
			if(password.size() == 1){
				List<String> tempres = new ArrayList<String>();
				tempres.add(BCrypt.hashpw(password.get(0), BCrypt.gensalt(logRounds)));
				return tempres;
			}
			else{
				//multiple password
				String oneHash;
				//create multi thread for each server
				int expectThreadNum = Math.min(4, password.size());
				ExecutorService executorService = Executors.newFixedThreadPool(expectThreadNum);
				int currentP = 0;
				int passwordsPerThread = password.size()/expectThreadNum;
				String[] result = new String[password.size()];
				for(int i = 0; i < expectThreadNum; i++){
					int head = i * passwordsPerThread;
					int tail = i < expectThreadNum - 1 ? (i + 1) * passwordsPerThread : password.size();
					executorService.submit(() -> {
						for(int x = head; x < tail; x++){
							result[x] = BCrypt.hashpw(password.get(x), BCrypt.gensalt(logRounds));
						}
					});

				}
				
				executorService.shutdown();
				executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
				return Arrays.asList(result);
			}
			


		} catch (Exception e) {
			throw new IllegalArgument(e.getMessage());
		}
	}

	public List<Boolean> checkPasswordFunction(List<String> password, List<String> hash) throws IllegalArgument, org.apache.thrift.TException
	{
		try {
			if(password.size() == 1){
				
				List<Boolean> tempres = new ArrayList<Boolean>();
				tempres.add(BCrypt.checkpw(password.get(0), hash.get(0)));
				return tempres;
			}
			else{
				//multiple password
				int expectThreadNum = Math.min(4, password.size());
				ExecutorService executorService = Executors.newFixedThreadPool(expectThreadNum);
				int currentP = 0;
				int passwordsPerThread = password.size()/expectThreadNum;

				Boolean[] result = new Boolean[password.size()];
				
				for(int i = 0; i < expectThreadNum; i++){
					int head = i * passwordsPerThread;
					int tail = i < expectThreadNum - 1 ? (i + 1) * passwordsPerThread : password.size();
					executorService.submit(() -> {
						for(int x = head; x < tail; x++){
							String onePwd;
							String oneHash;
							onePwd = password.get(x);
							oneHash = hash.get(x);
							result[x] = BCrypt.checkpw(onePwd, oneHash);
							
						}
					});
				}
				executorService.shutdown();
				executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
				return Arrays.asList(result);
			}
			

		} catch (Exception e) {
			throw new IllegalArgument(e.getMessage());
		}
	}
	
	public boolean BEconnected(String host, int port) throws IllegalArgument, org.apache.thrift.TException{
		try {		
			workerPool.add(new Worker(host, port, false));
			return true;
		} catch (Exception e) {
			throw new IllegalArgument(e.getMessage());
		}
	}

	public int workloadCal(List<String> password, short logRounds){
		return (int) (password.size() * Math.pow(2, logRounds));
	}

	public static boolean isBcryptHashMalformed(String hash) {
        String bcryptPattern = "^\\$2a\\$([0-3][0-9]|\\d)\\$[./0-9A-Za-z]{22}[./0-9A-Za-z]{31}$";

        return !Pattern.matches(bcryptPattern, hash);
    }
}
