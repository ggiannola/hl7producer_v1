import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class HL7MLLPClient {

    private static final String HOSTNAME = "localhost";
    private static final int PORT = 6661;

    private static final char MLLP_START = 0x0b;
    private static final char MLLP_END = 0x1c;
    private static final char MLLP_LAST = 0x0d;

    private static final List<String> MESSAGES = Arrays.asList(
            // ADT message
            "MSH|^~\\&|HIS|RIH|EKG|EKG|200708181126||ADT^A01|1049691901|P|2.4\n" +
            "EVN|A01|200708181123||\n" +
            "PID|||555-44-4444||Doe^John^Edward^III^Mr.|Smith^Mary|19600407|M||C|1200 N Elm St^^Greensboro^NC^27401-1020|GL|(919)379-1212|(919)271-3434||S||123456789|987-65-4320|||B-||N\n" +
            "PV1||I|W^389^1^UABH^^^^3|||X^Referring^L^Dr.^|^Consulting^Larry^Lee^Dr.^|||||||Medical|||None|||||||||||||||||||||||||19600407||\n",

            // ORU message
            "MSH|^~\\&|LIS|RIH|EKG|EKG|200708181123||ORU^R01|123456|P|2.4\n" +
            "PID|||555-44-4444||Doe^John^Edward^III^Mr.|Smith^Mary|19600407|M||C|1200 N Elm St^^Greensboro^NC^27401-1020|GL|(919)379-1212|(919)271-3434||S||123456789|987-65-4320|||B-||N\n" +
            "OBR|1|845439^GHH OE|1045813^GHH LAB|1554-5^GLUCOSE^POST 12H CFST:MCNC:PT:SER/PLAS:QN|||200708181123|||L||||||555-55-5555^PRIMARY^PATRICIA P^^^^^^^L|||||200708181206|||F\n" +
            "OBX|1|SN|1554-\n",

            // ORM message
            "MSH|^~\\&|OrderSys|XYZHospital|LabSys|XYZHospital|202403180830||ORM^O01|1234|P|2.4\n" +
            "PID|1||1234567890^^^XYZHospital^MR||Doe^Jane^M||19800101|F|||123 Main St.^^Metropolis^IL^62960||555-555-0123|||MARRIED|||999-99-9999\n" +
            "PV1|1|O|OPD||||1234^Doe^John^^^^^DR|||MED||||2|||1234^Doe^John^^^^^DR|E|||||||||||||||||||XYZHospital|||||202403180830||||||\n" +
            "ORC|NW|1234567890^OrderSys||999999^LabSys|||||||||1234^Doe^John^^^^^DR\n" +
            "OBR|1|1234567890^OrderSys|999999^LabSys|100^Complete Blood Count^XYZLab|||202403180830||||||Blood draw scheduled for 202403181030||||1234^Doe^John^^^^^DR||123456^Fingerstick^XYZLab||202403181000||LAB|F\n",

            // SIU message
            "MSH|^~\\&|SchedulingSys|XYZHospital|ReceptionSys|XYZHospital|202403180830||SIU^S12|5678|P|2.4\n" +
            "SCH|12345|67890|1^Consultation^XYZDept|45^min|^^^202403201030^202403201115|||1^new appointment|||||123^Smith^John^J^^^Dr.||Scheduled\n" +
            "PID|1||1234567890^^^XYZHospital^MR||Doe^Jane^M||19800101|F|||123 Main St.^^Metropolis^IL^62960||555-555-0123|||MARRIED|||999-99-9999\n" +
            "RGS|1|||XYZHospital\n" +
            "AIS|1||100^Neurology Consultation^XYZDept|^^^202403201030^202403201115|45^min|||123^Smith^John^J^^^Dr.|Scheduled\n" +
            "AIP|1||123^Smith^John^J^^^Dr.|^^^202403201030^202403201115|45^min||Scheduled\n"
    );

    public static void main(String[] args) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendRandomHL7Message();
            }
        }, 0, 300000); // Schedule to run every 5 minutes
    }

    private static void sendRandomHL7Message() {
        Random random = new Random();
        int index = random.nextInt(MESSAGES.size());
        String message = MESSAGES.get(index);

        String hl7Message = MLLP_START + message + MLLP_END + MLLP_LAST;

        try (Socket socket = new Socket(HOSTNAME, PORT);
             OutputStream os = socket.getOutputStream();
             InputStream is = socket.getInputStream()) {

            // Prepare the HL7 message with MLLP framing characters
            String mllpMessage = MLLP_START + hl7Message + MLLP_END + MLLP_LAST;
            // Sending the HL7 message
            os.write(mllpMessage.getBytes());
            os.flush();
            System.out.println("HL7 message sent through MLLP:\n" + hl7Message);

            // Reading the response from the server
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            boolean endOfMessage = false;

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
                if (buffer.toString().contains("" + MLLP_END + MLLP_LAST)) {
                    endOfMessage = true;
                    break; // Stop reading once the end-of-message sequence is found
                }
            }

            if (!endOfMessage) {
                System.out.println("Did not receive proper end-of-message indication.");
                return;
            }

            // Extract the response from the buffer
            String response = buffer.toString();
            int startIndex = response.indexOf("" + MLLP_START) + 1;
            int endIndex = response.indexOf("" + MLLP_END);
            if (startIndex > 0 && endIndex > startIndex) {
                String hl7Response = response.substring(startIndex, endIndex);
                System.out.println("Received response from server:\n" + hl7Response);
            } else {
                System.out.println("Could not extract HL7 message from response.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("An error occurred while sending the HL7 message or reading the response.");
        }

    }
}