/*
* Copyright (c) Joan-Manuel Marques 2013. All rights reserved.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
*
* This file is part of the practical assignment of Distributed Systems course.
*
* This code is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This code is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this code.  If not, see <http://www.gnu.org/licenses/>.
*/

package recipes_service.tsae.data_structures;



import java.io.Serializable;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import edu.uoc.dpcs.lsim.logger.LoggerManager.Level;
import lsim.library.api.LSimLogger;
import lsim.library.api.LSimWorker;
import lsim.library.api.LSimFactory;

/**
 * @author Joan-Manuel Marques
 * December 2012
 *
 */
public class TimestampVector implements Serializable{
	// Only for the zip file with the correct solution of phase1.Needed for the logging system for the phase1. sgeag_2018p 
//	private transient LSimCoordinator lsim = LSimFactory.getCoordinatorInstance();
	// Needed for the logging system sgeag@2017
	//private transient LSimWorker lsim = LSimFactory.getWorkerInstance();
	
	private static final long serialVersionUID = -765026247959198886L;
	/**
	 * This class stores a summary of the timestamps seen by a node.
	 * For each node, stores the timestamp of the last received operation.
	 */
	
	private ConcurrentHashMap<String, Timestamp> timestampVector= new ConcurrentHashMap<String, Timestamp>();
	
	public TimestampVector (List<String> participants){
		// create and empty TimestampVector
		for (Iterator<String> it = participants.iterator(); it.hasNext(); ){
			String id = it.next();
			// when sequence number of timestamp < 0 it means that the timestamp is the null timestamp
			timestampVector.put(id, new Timestamp(id, Timestamp.NULL_TIMESTAMP_SEQ_NUMBER));
		}
	}

	/**
	 * Updates the timestamp vector with a new timestamp. 
	 * @param timestamp
	 */
	public void updateTimestamp(Timestamp timestamp){
		if (timestamp != null && !timestamp.isNullTimestamp()) {
			String hostid = timestamp.getHostid();
			Timestamp currentTimestamp = timestampVector.get(hostid);
			
			// Update if the new timestamp is newer than the current one
			// or if there's no current timestamp for this host
			if (currentTimestamp == null || timestamp.compare(currentTimestamp) > 0) {
				timestampVector.put(hostid, timestamp);
			}
		}
	}
	
	/**
	 * merge in another vector, taking the elementwise maximum
	 * @param tsVector (a timestamp vector)
	 */
	public void updateMax(TimestampVector tsVector){
		if (tsVector == null) {
			return;
		}
		
		// Iterate through all nodes in the other vector
		for (String hostid : tsVector.timestampVector.keySet()) {
			Timestamp otherTimestamp = tsVector.timestampVector.get(hostid);
			Timestamp currentTimestamp = this.timestampVector.get(hostid);
			
			// Update with the maximum timestamp for each node
			if (currentTimestamp == null || 
			    (otherTimestamp != null && otherTimestamp.compare(currentTimestamp) > 0)) {
				this.timestampVector.put(hostid, otherTimestamp);
			}
		}
	}
	
	/**
	 * 
	 * @param node
	 * @return the last timestamp issued by node that has been
	 * received.
	 */
	public Timestamp getLast(String node){
		
		return this.timestampVector.get(node);
	}
	
	/**
	 * merges local timestamp vector with tsVector timestamp vector taking
	 * the smallest timestamp for each node.
	 * After merging, local node will have the smallest timestamp for each node.
	 *  @param tsVector (timestamp vector)
	 */
	public void mergeMin(TimestampVector tsVector){
		if (tsVector == null) {
			return;
		}
		
		// Iterate through all nodes in the local vector
		for (String hostid : this.timestampVector.keySet()) {
			Timestamp currentTimestamp = this.timestampVector.get(hostid);
			Timestamp otherTimestamp = tsVector.timestampVector.get(hostid);
			
			// Update with the minimum timestamp for each node
			if (otherTimestamp != null) {
				if (currentTimestamp == null || otherTimestamp.compare(currentTimestamp) < 0) {
					this.timestampVector.put(hostid, otherTimestamp);
				}
			}
		}
	}
	
	/**
	 * clone
	 */
	public TimestampVector clone(){
		List<String> participants = new java.util.ArrayList<String>(this.timestampVector.keySet());
		TimestampVector clonedVector = new TimestampVector(participants);
		
		// Copy all timestamps from this vector to the cloned vector
		for (String hostid : this.timestampVector.keySet()) {
			Timestamp timestamp = this.timestampVector.get(hostid);
			if (timestamp != null) {
				clonedVector.timestampVector.put(hostid, timestamp);
			}
		}
		
		return clonedVector;
	}
	
	/**
	 * equals
	 */
	public boolean equals(Object obj){
		// Check for identity
		if (this == obj) {
			return true;
		}
		
		// Check for null
		if (obj == null) {
			return false;
		}
		
		// Check for correct type
		if (!(obj instanceof TimestampVector)) {
			return false;
		}
		
		// Cast to TimestampVector
		TimestampVector other = (TimestampVector) obj;
		
		// Check if both vectors have the same structure
		if (this.timestampVector == null) {
			return other.timestampVector == null;
		}
		
		// Check if they have the same number of nodes
		if (this.timestampVector.size() != other.timestampVector.size()) {
			return false;
		}
		
		// Check each timestamp in the vector
		for (String hostid : this.timestampVector.keySet()) {
			Timestamp thisTimestamp = this.timestampVector.get(hostid);
			Timestamp otherTimestamp = other.timestampVector.get(hostid);
			
			// If the other vector doesn't have this host, they're not equal
			if (!other.timestampVector.containsKey(hostid)) {
				return false;
			}
			
			// Compare timestamps
			if (thisTimestamp == null) {
				if (otherTimestamp != null) {
					return false;
				}
			} else {
				if (!thisTimestamp.equals(otherTimestamp)) {
					return false;
				}
			}
		}
		
		return true;
	}

	/**
	 * toString
	 */
	@Override
	public synchronized String toString() {
		String all="";
		if(timestampVector==null){
			return all;
		}
		for(Enumeration<String> en=timestampVector.keys(); en.hasMoreElements();){
			String name=en.nextElement();
			if(timestampVector.get(name)!=null)
				all+=timestampVector.get(name)+"\n";
		}
		return all;
	}
}
