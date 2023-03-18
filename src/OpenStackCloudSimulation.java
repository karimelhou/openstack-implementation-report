package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class OpenStackCloudSimulation {
    private static final int NUM_HOSTS = 1; // Number of physical hosts
    private static final int NUM_VMS = 2; // Number of virtual machines
    private static final int RAM_SIZE = 7000; // Amount of RAM in the physical host in MB
    private static final int CPU_MIPS = 2000; // MIPS capacity of the physical host
    private static final int BANDWIDTH = 100000; // Bandwidth between hosts in Mbps

    public static void main(String[] args) {
        try {
            int num_user = 1;   // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace GridSim events

            CloudSim.init(num_user, calendar, trace_flag);

            // Create datacenter
            Datacenter datacenter = createDatacenter("OpenStack");

            // Create broker
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            // Create virtual machines
            List<Cloudlet> cloudletList = new LinkedList<>();
            List<Vm> vmList = new ArrayList<>();
            for(int i=0; i<NUM_VMS; i++) {
                Vm vm = createVM(i, brokerId);
                vmList.add(vm);

                // Create cloudlets and bind them to the VMs
                Cloudlet cloudlet = createCloudlet(i, brokerId);
                cloudlet.setVmId(vm.getId());
                cloudletList.add(cloudlet);
            }

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            printCloudletList(newList);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error in simulation");
        }
    }

    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<>();

        // Create the host
        for(int i=0; i<NUM_HOSTS; i++) {
            List<Pe> peList = new ArrayList<>();
            peList.add(new Pe(0, new PeProvisionerSimple(CPU_MIPS)));

            Host host = new Host(i, new RamProvisionerSimple(RAM_SIZE), new BwProvisionerSimple(BANDWIDTH), 250000, peList, new VmSchedulerSpaceShared(peList));
            hostList.add(host);
        }

        String arch = "x86";      // system architecture
        String os = "Linux";          // operating system
        String vmm = "Xen";
        double time_zone = 10.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using processing in this resource
        double costPerMem = 0.05;        // the cost of using memory in this resource
        double costPerStorage = 0.1;
        double costPerBw = 0.1; // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<>(); // we are not adding SAN devices by now
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        // Finally, create the datacenter
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datacenter;
    }

    private static DatacenterBroker createBroker() {
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    private static Vm createVM(int id, int brokerId) {
        int mips = CPU_MIPS;
        long size = 10000; // image size (MB)
        int ram = 1024; // vm memory (MB)
        int bw = 1000;

        Vm vm = new Vm(id, brokerId, mips, 1, ram, bw, size, "Xen", new CloudletSchedulerTimeShared());
        return vm;
    }

    private static Cloudlet createCloudlet(int id, int brokerId) {
        long length = 40000; // cloudlet length
        int pesNumber = 1;
        long fileSize = 300;
        long outputSize = 300;
        UtilizationModel utilizationModel = new UtilizationModelStochastic();

        Cloudlet cloudlet = new Cloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
        cloudlet.setUserId(brokerId);
        return cloudlet;
    }

    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;
        String indent = "    ";
        System.out.println();
        System.out.println("========== OUTPUT ==========");
        System.out.println("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            System.out.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                System.out.print("SUCCESS");

                System.out.println(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() + indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime()) + indent + indent + dft.format(cloudlet.getFinishTime()));
            }
        }
    }
}
