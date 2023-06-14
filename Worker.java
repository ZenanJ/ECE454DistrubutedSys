import java.util.ArrayList;
import java.util.List;

import javax.print.attribute.standard.MediaSize.ISO;

import org.mindrot.jbcrypt.BCrypt;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TTransportFactory;

public class Worker {

    private int hashLoad = 0;
    private int checkLoad = 0;
    private String host;
    private int port;
    private boolean isFrontend;
    public Worker(String hostIn, int portIn, boolean isFE){
        host = hostIn;
        port = portIn;
        isFrontend = isFE;
    }

    public Boolean isFrontend(){
        return isFrontend;
    }


    public int getWorkload(){
        return hashLoad;
    }

    public int getCheckload(){
        return checkLoad;
    }

    public void addCheckload(int newWorkload){
        checkLoad += newWorkload;
    }

    public void removeCheckload(int finishedWorkload){
        checkLoad -= finishedWorkload;
    }

    public void addWorkload(int newWorkload){
        hashLoad += newWorkload;
    }

    public void removeWorkload(int finishedWorkload){
        hashLoad -= finishedWorkload;
    }

    public BcryptService.Client getWorkerClient(){
        TSocket sock = new TSocket(host, port);
		TTransport transport = new TFramedTransport(sock);
		TProtocol protocol = new TBinaryProtocol(transport);
		return new BcryptService.Client(protocol);
    }
}
    
