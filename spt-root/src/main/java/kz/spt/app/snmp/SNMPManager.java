package kz.spt.app.snmp;

import java.io.IOException;
import java.text.ParseException;

import lombok.extern.java.Log;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

@Log
public class SNMPManager {

    private Snmp snmp = null;
    private String address = null;
    private String security = null;
    private TransportMapping transport = null;

    public SNMPManager(String address, String security)
    {
        this.address = address;
        this.security = security;
    }

    public void start() throws IOException {
        transport = new DefaultUdpTransportMapping();
        snmp = new Snmp(transport);
        snmp.listen();
    }

    public Boolean open(String oidString) throws IOException {
        OID openIod = new OID(oidString);
        OID[] oids = new OID[] { openIod};
        PDU pdu = new PDU();
        for (OID oid : oids) {
            pdu.add(new VariableBinding(oid, new Integer32(1)));
        }
        pdu.setType(PDU.SET);

        ResponseEvent event = snmp.send(pdu, getTarget(), null);

        if(event != null && event.getResponse()!=null && event.getResponse().get(0)!=null && event.getResponse().get(0).getVariable()!=null){
            return "1".equals(event.getResponse().get(0).getVariable().toString());
        }
        return false;
    }

    public void close() throws IOException, ParseException{
        snmp.close();
        transport.close();
    };

    private Target getTarget() {
        Address targetAddress = GenericAddress.parse(address);
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(this.security));
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);
        return target;
    }

}
