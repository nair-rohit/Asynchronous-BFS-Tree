package com.async;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgramDriver {

    public static Map<ChannelKey, Deque<Message>> channelMap = new ConcurrentHashMap<>();
    public static Map<Integer, List<Integer>> outputList = new ConcurrentHashMap<>();
    public static Map<Integer,List<Integer>> neighbourMap = new HashMap<>();
    public static AtomicInteger messageCount = new AtomicInteger(0);
    public static AtomicInteger completedCount = new AtomicInteger(0);

    public static void incrementMessageCount(int val){
        messageCount.addAndGet(val);
    }
    public static void incrementCompletedCount(){
        completedCount.addAndGet(1);
    }

    public static void main(String[] args) {

        try {
            BufferedReader inputReader = new BufferedReader(new FileReader(new File("tp.txt")));

            int processCount = -1, leaderId = -1;
            String s = inputReader.readLine();
            ArrayList<String> input = new ArrayList<>();
            while (s != null) {
                input.add(s);
                s = inputReader.readLine();
            }

            processCount = Integer.valueOf(input.get(0));

            // Saving Node/Process Id
            int[] ids = new int[processCount];
            for (int i = 0; i < processCount; i++) {
                ids[i] = i;
                ChannelKey ck = new ChannelKey(i, 1000);
                channelMap.put(ck, new ArrayDeque<Message>());
            }

            int root = Integer.valueOf(input.get(1));
            root=root-1;
            int k = 2;
            for(int i=0;i<processCount;i++){
                int neighbourIndex = 0;
                List<Integer> neighbour = new ArrayList<>();
                for(String elem: input.get(k++).split(" ")){

                    if("1".equals(elem)){
                        ChannelKey ck = new ChannelKey(i, neighbourIndex);
                        channelMap.put(ck, new ArrayDeque<Message>());
                        neighbour.add(neighbourIndex);
                    }
                    neighbourIndex++;
                    neighbourMap.put(i, neighbour);
                }
            }
            //has read the input file till here

            //following piece creates the runnable objects and tries to run it
            Runner[] rarr = new Runner[processCount];
            for(int i=0;i<processCount;i++){
                rarr[i] = new Runner(i, neighbourMap.get(i), root, processCount);
            }
            Thread[] arr = new Thread[processCount];
            for(int i=0;i<processCount;i++){
                arr[i] = new Thread(rarr[i]);
                arr[i].start();
            }

            while(!(completedCount.intValue()==processCount)){
            }

            for (int i = 0; i < processCount; i++) {
                arr[i].stop();
            }

            for(Map.Entry<Integer, List<Integer>> entry:outputList.entrySet()){

                for(int elem:entry.getValue()){
                    System.out.println((entry.getKey()+1) + " -> "+(elem+1));
                }
            }
            System.out.println("#MAIN# count of messages : " + messageCount.intValue());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
