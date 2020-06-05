package com.async;

import java.util.*;
import java.util.stream.Collectors;

public class Runner implements Runnable {

    int processId;
    int rootId;
    List<Integer> neighbourList;
    int distanceFromRoot=0;
    int prevDistance = 0;
    Integer parent;
    boolean processingDone = false;
    int nackCount = 0;
    int ackCount = 0;
    int messageSentCount=0;
    int processCount = 0;
    Set<ChannelKey> nackedChannelKey = new HashSet<>();
    Set<Integer> messageSentAlready = new HashSet<>();

    public Runner(int processId, List<Integer> neighbourList, int rootId, int processCount) {
        this.processId = processId;
        this.neighbourList = neighbourList;
        this.rootId = rootId;
        this.processCount = processCount;
    }

    @Override
    public void run() {
        List<ChannelKey> inputCkList = new ArrayList<>();
        List<ChannelKey> outputCkList = new ArrayList<>();
        for(int elem:neighbourList){
            inputCkList.add(new ChannelKey(elem, processId));
            outputCkList.add(new ChannelKey(processId, elem));
        }
        if(rootId==processId){
            sendMessage(processId, neighbourList, null, 0);
            while(ProgramDriver.completedCount.intValue()!=(processCount-1)){
                processReceivedMessage(inputCkList);
            }
            ProgramDriver.incrementCompletedCount();
            return;
        }
        while(!processingDone) {
            List<ChannelKey> ck = new ArrayList<>();
            int count = 0;
            do {
                    ck = findIfProcessingToBeTriggered(processId, inputCkList);

                } while (ck.isEmpty());
            processReceivedMessage(ck);
            if(!processingDone && messageSentCount!=neighbourList.size())
                sendMessage(processId, neighbourList, parent, distanceFromRoot);
            ck.clear();
        }

    }

    private void processReceivedMessage(List<ChannelKey> ck) {
        for(ChannelKey channel:ck) {

            Deque<Message> q = ProgramDriver.channelMap.get(channel);
            while(!q.isEmpty()) {
                Message msg = q.removeFirst();

                if(processId==rootId && "PARENT".equals(msg.getText())){
                    //System.out.println("#PROCESS_RECEIVE# Thread " + processId + "[" + msg + "]: size[" + ProgramDriver.channelMap.get(channel).size() + "]");
                    ProgramDriver.channelMap.get(new ChannelKey(channel.getDestination(), channel.getSource()))
                            .addLast(new Message("NACK", channel.getDestination(), channel.getSource(), null));
                    ProgramDriver.incrementMessageCount(1);
                    continue;
                }

                //System.out.println("#PROCESS_RECEIVE# Thread " + processId + "[" + msg + "]: size[" + ProgramDriver.channelMap.get(channel).size() + "]");
                Integer newDistance = msg.getDistanceFromRoot();
                if ("NACK".equals(msg.getText())) {
                    //TODO: Whenever NACK is received the node knows its not a parent.
                    // If its equal to the size of neighbour list, then its a leaf and can respond ACK to parent.
                    nackCount++;
                    nackedChannelKey.add(channel);
                    //System.out.println("Thread " + processId +" nacked[" + nackedChannelKey+"]");
                    neighbourList.removeAll(nackedChannelKey.stream().map(ChannelKey::getSource).collect(Collectors.toList()));
                    //System.out.println("Thread " + processId +":neigh:" + neighbourList);
                }else if ("ACK".equals(msg.getText())) {
                    //TODO:Subtree from here is done. And so processng is done for this root.
                    ackCount++;
                    List<Integer> childrenList = ProgramDriver.outputList.get(processId);
                    if(childrenList==null) {
                        childrenList = new ArrayList<>();
                        childrenList.add(msg.getSource());
                        //System.out.println("#CHILDREN# Thread " +processId + childrenList);
                        ProgramDriver.outputList.put(processId, childrenList);
                    }else{
                        ProgramDriver.outputList.get(processId).add(msg.getSource());
                        //System.out.println("#CHILDREN# Thread " +processId + ProgramDriver.outputList.get(processId));
                    }
                    neighbourList.removeAll(childrenList);
                    //System.out.println("Thread " + processId +":neigh:" +neighbourList);

                }
                else if (processId!=rootId && parent == null || (newDistance != null && distanceFromRoot > newDistance)) {
                    prevDistance = distanceFromRoot;
                    distanceFromRoot = newDistance;
                    int oldP = -1;
                    if (parent != null) {
                        //System.out.println("#PROCESS_PARENT_CHANGE# Thread " + processId + " : " + msg.getSource() + " :oldP[" + oldP + "]");
                        ProgramDriver.channelMap.get(new ChannelKey(processId, parent)).addLast(new Message("NACK", processId, parent, null));
                        ProgramDriver.incrementMessageCount(1);
                        oldP = parent;
                    }
                    messageSentCount = 0;
                    parent = msg.getSource();
                    nackCount = 0;
                    messageSentAlready.clear();
                    //System.out.println("#PARENT_ASSIGNED# Thread " + processId + "[" + parent + "] from [" + oldP + "]");
                } else if (parent != null && parent != msg.getSource() && (newDistance == null || distanceFromRoot <= newDistance)) {
                    ProgramDriver.channelMap.get(new ChannelKey(channel.getDestination(), channel.getSource()))
                            .addLast(new Message("NACK", channel.getDestination(), channel.getSource(), null));
                    ProgramDriver.incrementMessageCount(1);
                }
            }
        }
        if(parent!=null && neighbourList.isEmpty()){
            //System.out.println("Thread " + processId + " :neigh: " + neighbourList.size());
            ProgramDriver.channelMap.get(new ChannelKey(processId, parent)).addLast(new Message("ACK", processId, parent, null));
            ProgramDriver.incrementMessageCount(1);
            ProgramDriver.incrementCompletedCount();
            //System.out.println("#COMPLETED_COUNT# Thread " + processId + " : " + ProgramDriver.completedCount.intValue());
            processingDone = true;
            Thread.yield();
        }
    }

    private List<ChannelKey> findIfProcessingToBeTriggered(int processId, List<ChannelKey> inputCkList) {
        List<ChannelKey> ckList = new ArrayList<>();

        for(ChannelKey ck: inputCkList){
            if(!nackedChannelKey.contains(ck) && ProgramDriver.channelMap.get(ck).size()>0){
                //System.out.println("#CHECK_PROCESSING_TRIGGER# Thread "+processId +"["+ ck + "] : size[" + ProgramDriver.channelMap.get(ck).size()+"]");
                ckList.add(ck);
            }
        }
        return ckList;
    }

    private void sendMessage(int processId, List<Integer> neighbourList, Integer parent, int distanceFromRoot) {

        //System.out.println("Thread " + processId + ":" + messageSentAlready);
        if(neighbourList.isEmpty())
            return;
        int msgCount=0;
        for(int elem:neighbourList){
            if(parent!=null && elem==parent && messageSentAlready.contains(elem))
                continue;
            ChannelKey ck = new ChannelKey(processId, elem);
            Deque<Message> q = ProgramDriver.channelMap.get(ck);

            if(processId==rootId) {
                messageSentAlready.add(elem);
                q.addLast(new Message("ROOT", processId, elem, distanceFromRoot + 1));
                msgCount++;
            }
            else {
                messageSentAlready.add(elem);
                q.addLast(new Message("PARENT", processId, elem, distanceFromRoot + 1));
                msgCount++;
            }
        }
        messageSentCount = msgCount;
        ProgramDriver.incrementMessageCount(msgCount);
    }
}
