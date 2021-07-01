package com.vipgp.tinyurl.dubbo.provider.consistenthash;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * author: linshangdou@gmail.com
 * date: 2021/1/13
 */
public class ConsistentHashProvider<T extends Node> {

    private final SortedMap<Long, VirtualNode<T>> ring = new TreeMap<>();
    private final Hashable hashFunction;

    public ConsistentHashProvider(Collection<T> pNodes, int vNodeCount) {
        this(pNodes,vNodeCount, new MurMurHash());
    }

    /**
     *
     * @param pNodes collections of physical nodes
     * @param vNodeCount amounts of virtual nodes
     * @param hashFunction hash Function to hash Node instances
     */
    public ConsistentHashProvider(Collection<T> pNodes, int vNodeCount, Hashable hashFunction) {
        if (hashFunction == null) {
            throw new NullPointerException("Hash Function is null");
        }
        this.hashFunction = hashFunction;
        if (pNodes != null) {
            for (T pNode : pNodes) {
                addNode(pNode, vNodeCount);
            }
        }
    }

    /**
     * add physic node to the hash ring with some virtual nodes
     * @param pNode physical node needs added to hash ring
     * @param vNodeCount the number of virtual node of the physical node. Value should be greater than or equals to 0
     */
    public void addNode(T pNode, int vNodeCount) {
        if (vNodeCount < 0) {throw new IllegalArgumentException("illegal virtual node counts :" + vNodeCount);}
        int existingReplicas = getExistingReplicas(pNode);
        System.out.println("pNode="+pNode.getKey()+",existingReplicas= " +existingReplicas);
        for (int i = 0; i < vNodeCount; i++) {
            VirtualNode<T> vNode = new VirtualNode<>(pNode, i + existingReplicas);
            ring.put(hashFunction.hash(vNode.getKey()), vNode);
        }
    }

    /**
     * remove the physical node from the hash ring
     * @param pNode
     */
    public void removeNode(T pNode) {
        Iterator<Long> it = ring.keySet().iterator();
        while (it.hasNext()) {
            Long key = it.next();
            VirtualNode<T> virtualNode = ring.get(key);
            if (virtualNode.isVirtualNodeOf(pNode)) {
                it.remove();
            }
        }
    }

    /**
     * with a specified key, route the nearest Node instance in the current hash ring
     * @param objectKey the object key to find a nearest Node
     * @return
     */
    public T routeNode(String objectKey) {
        if (ring.isEmpty()) {
            return null;
        }
        Long hashVal = hashFunction.hash(objectKey);
        SortedMap<Long,VirtualNode<T>> tailMap = ring.tailMap(hashVal);
        Long nodeHashVal = !tailMap.isEmpty() ? tailMap.firstKey() : ring.firstKey();
        return ring.get(nodeHashVal).getPhysicalNode();
    }


    public int getExistingReplicas(T pNode) {
        int replicas = 0;
        for (VirtualNode<T> vNode : ring.values()) {
            if (vNode.isVirtualNodeOf(pNode)) {
                replicas++;
            }
        }
        return replicas;
    }

}
