import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TTransportFactory;

public class Client {
	public static void main(String [] args) {
		if (args.length != 2) {
			System.err.println("Usage: java Client FE_host FE_port password");
			System.exit(-1);
		}

		try {
			
			TSocket sock = new TSocket(args[0], Integer.parseInt(args[1]));
			TTransport transport = new TFramedTransport(sock);
			TProtocol protocol = new TBinaryProtocol(transport);
			BcryptService.Client client = new BcryptService.Client(protocol);
			transport.open();

			List<String> password = new ArrayList<>();
			for(int x = 2; x < 18; x ++){
                byte bytes[] = new byte[1024];
			    Random rand = new Random();
			    rand.nextBytes(bytes);
			    String password212 = new String(bytes);
				password.add(password212);
			}
			long startTime = System.currentTimeMillis();
			List<String> hash = client.hashPassword(password, (short)10);
			long endTime = System.currentTimeMillis();
            System.out.println("Throughput for logRounds=" + 10 + ": " + 16 * 1000f/(endTime-startTime));
			System.out.println("Latency for logRounds=" + 10 + ": " + (endTime-startTime)/16);
			System.out.println(endTime-startTime);
			//System.out.println("Password: " + password.toString());
			//System.out.println("Hash: " + hash.toString());
			System.out.println("Positive check: " + client.checkPassword(password, hash));
			for(int x = 0; x < 16; x ++){
				hash.set(x, "$2a$14$reBHJvwbb0UWqJHLyPTVF.6Ld5sFRirZx/bXMeMmeurJledKYdZmG");
			}
			System.out.println("Negative check: " + client.checkPassword(password, hash));
			try {
				hash.set(0, "too short");
				List<Boolean> rets = client.checkPassword(password, hash);
				System.out.println("Exception check: no exception thrown");
			} catch (Exception e) {
				System.out.println("Exception check: exception thrown");
			}

			transport.close();
			
		} catch (TException x) {
			x.printStackTrace();
		} 
	}
}

// import java.util.List;
// import java.util.Random;
// import java.util.ArrayList;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.CountDownLatch;

// import org.apache.thrift.TException;
// import org.apache.thrift.protocol.TProtocol;
// import org.apache.thrift.protocol.TBinaryProtocol;
// import org.apache.thrift.transport.TTransport;
// import org.apache.thrift.transport.TSocket;
// import org.apache.thrift.transport.TFramedTransport;
// import org.apache.thrift.transport.TTransportFactory;

// public class Client extends Thread{
//     private String _host;
//     private int _port;
//     private CountDownLatch _latch;
//     private List<String> _passwordList;

// 	public static List<String> getPasswordList() {
// 		List<String> passwordList = new ArrayList<>();
// 		for(int i = 0; i <1; i++){
// 			byte bytes[] = new byte[1024];
// 			Random rand = new Random();
// 			rand.nextBytes(bytes);
// 			String password = new String(bytes);
// 			passwordList.add(password);
// 		}
        
// 		return passwordList;
// 	}

//     public Client(String host, int port, CountDownLatch latch, List<String> passwordList) {
//         this._host = host;
//         this._port = port;
//         this._latch = latch;
//         this._passwordList = passwordList;
//     }

//     public void run() {
//         try {
//             TSocket sock = new TSocket(this._host, this._port);
//             TTransport transport = new TFramedTransport(sock);
//             TProtocol protocol = new TBinaryProtocol(transport);
//             BcryptService.Client client = new BcryptService.Client(protocol);
//             transport.open();
//             // System.out.println("passwordList: " + passwordList.toString());
//             List<String> hash = client.hashPassword(_passwordList, (short) 10);
//             // System.out.println("hash: " + hash.toString());
//             try {
//                 //System.out.println("Positive check: " + client.checkPassword(_passwordList, hash));
//                 // System.out.println("Exception check: no exception thrown");
//             } catch (Exception e) {
//                 // System.out.println("Exception check: exception thrown");
//             } finally {
//                 this._latch.countDown();
//                 transport.close();
//             }
//         } catch (TException x) {
//             x.printStackTrace();
//         }
//     }

// 	public static void main(String[] args) {
// 		if (args.length != 2) {
// 			System.err.println("Usage: java Client FE_host FE_port");
// 			System.exit(-1);
// 		}
//         List<String> passwordList = getPasswordList();
//         CountDownLatch latch = new CountDownLatch(16);
//         long startTime = System.currentTimeMillis();
//         for (int i = 0; i < 16; i++) {
//             new Client(args[0], Integer.parseInt(args[1]), latch, passwordList).start();
//         }
//         try {
//             latch.await();
//         } catch (InterruptedException e) {
//             // System.out.println(e);
//         }
//         long endTime = System.currentTimeMillis();
//         System.out.println("Finished in " + (endTime - startTime) + "ms");
//         System.out.println("Throughput: " + 16 * 1000f /(endTime - startTime));
// 	}
// }


// import java.util.List;
// import java.util.Random;
// import java.util.ArrayList;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.CountDownLatch;

// import org.apache.thrift.TException;
// import org.apache.thrift.protocol.TProtocol;
// import org.apache.thrift.protocol.TBinaryProtocol;
// import org.apache.thrift.transport.TTransport;
// import org.apache.thrift.transport.TSocket;
// import org.apache.thrift.transport.TFramedTransport;
// import org.apache.thrift.transport.TTransportFactory;

// public class Client extends Thread{
//     private String _host;
//     private int _port;
//     private CountDownLatch _latch;
//     private List<String> _passwordList;

// 	public static List<String> getPasswordList() {
// 		List<String> passwordList = new ArrayList<>();
// 		for(int i=0; i<4; i++){
// 			byte bytes[] = new byte[1024];
// 			Random rand = new Random();
// 			rand.nextBytes(bytes);
// 			String password = new String(bytes);
// 			passwordList.add(password);
// 		}
        
// 		return passwordList;
// 	}

//     public Client(String host, int port, CountDownLatch latch, List<String> passwordList) {
//         this._host = host;
//         this._port = port;
//         this._latch = latch;
//         this._passwordList = passwordList;
//     }

//     public void run() {
//         try {
//             TSocket sock = new TSocket(this._host, this._port);
//             TTransport transport = new TFramedTransport(sock);
//             TProtocol protocol = new TBinaryProtocol(transport);
//             BcryptService.Client client = new BcryptService.Client(protocol);
//             transport.open();
//             // System.out.println("passwordList: " + passwordList.toString());
//             List<String> hash = client.hashPassword(_passwordList, (short) 10);
//             // System.out.println("hash: " + hash.toString());
//             try {
//                 // System.out.println("Positive check: " + client.checkPassword(_passwordList, hash));
//                 // System.out.println("Exception check: no exception thrown");
//             } catch (Exception e) {
//                 // System.out.println("Exception check: exception thrown");
//             } finally {
//                 this._latch.countDown();
//                 transport.close();
//             }
//         } catch (TException x) {
//             x.printStackTrace();
//         }
//     }

// 	public static void main(String[] args) {
// 		if (args.length != 2) {
// 			System.err.println("Usage: java Client FE_host FE_port");
// 			System.exit(-1);
// 		}
//         List<String> passwordList = getPasswordList();
//         CountDownLatch latch = new CountDownLatch(4);
//         long startTime = System.currentTimeMillis();
//         for (int i = 0; i < 4; i++) {
//             new Client(args[0], Integer.parseInt(args[1]), latch, passwordList).start();
//         }
//         try {
//             latch.await();
//         } catch (InterruptedException e) {
//             // System.out.println(e);
//         }
//         long endTime = System.currentTimeMillis();
//         System.out.println("Finished in " + (endTime - startTime) + "ms");
//         System.out.println("Throughput: " + 16 * 1000f /(endTime - startTime));
// 	}
// }