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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import recipes_service.data.Operation;
//LSim logging system imports sgeag@2017
//import lsim.coordinator.LSimCoordinator;
import edu.uoc.dpcs.lsim.logger.LoggerManager.Level;
import lsim.library.api.LSimLogger;

/**
 * @author Joan-Manuel Marques, Daniel Lázaro Iglesias
 * December 2012
 *
 */
public class Log implements Serializable{
	// Only for the zip file with the correct solution of phase1.Needed for the logging system for the phase1. sgeag_2018p 
//	private transient LSimCoordinator lsim = LSimFactory.getCoordinatorInstance();
	// Needed for the logging system sgeag@2017
//	private transient LSimWorker lsim = LSimFactory.getWorkerInstance();

	private static final long serialVersionUID = -4864990265268259700L;
	/**
	 * This class implements a log, that stores the operations
	 * received  by a client.
	 * They are stored in a ConcurrentHashMap (a hash table),
	 * that stores a list of operations for each member of 
	 * the group.
	 */
	private ConcurrentHashMap<String, List<Operation>> log= new ConcurrentHashMap<String, List<Operation>>();  

	public Log(List<String> participants){
		// create an empty log
		for (Iterator<String> it = participants.iterator(); it.hasNext(); ){
			log.put(it.next(), new Vector<Operation>());
		}
	}

	/**
	 * inserts an operation into the log. Operations are 
	 * inserted in order. If the last operation for 
	 * the user is not the previous operation than the one 
	 * being inserted, the insertion will fail.
	 * 
	 * @param op
	 * @return true if op is inserted, false otherwise.
	 */
	public boolean add(Operation op){
		if (op == null || op.getTimestamp() == null) {
			return false;
		}
		
		Timestamp opTimestamp = op.getTimestamp();
		String hostid = opTimestamp.getHostid();
		
		// Get the list of operations for this host
		List<Operation> hostOperations = log.get(hostid);
		if (hostOperations == null) {
			return false; // Host not in participant list
		}
		
		// Check if this operation is the next expected operation
		if (hostOperations.isEmpty()) {
			// First operation for this host - should have sequence number 0
			if (opTimestamp.isNullTimestamp()) {
				return false; // Invalid timestamp
			}
			hostOperations.add(op);
			return true;
		} else {
			// Get the last operation's timestamp
			Operation lastOp = hostOperations.get(hostOperations.size() - 1);
			Timestamp lastTimestamp = lastOp.getTimestamp();
			
			// The new operation should be exactly one sequence number higher
			// compare() returns: this.seqnumber - other.seqnumber
			// We expect opTimestamp.seqnumber = lastTimestamp.seqnumber + 1
			// So opTimestamp.compare(lastTimestamp) should return 1
			if (opTimestamp.compare(lastTimestamp) == 1) {
				hostOperations.add(op);
				return true;
			} else {
				return false; // Not the next expected operation
			}
		}
	}
	
	/**
	 * Checks the received summary (sum) and determines the operations
	 * contained in the log that have not been seen by
	 * the proprietary of the summary.
	 * Returns them in an ordered list.
	 * @param sum
	 * @return list of operations
	 */
	public List<Operation> listNewer(TimestampVector sum){
		List<Operation> newerOperations = new Vector<Operation>();
		
		if (sum == null) {
			return newerOperations;
		}
		
		// Iterate through all hosts in the log
		for (String hostid : log.keySet()) {
			List<Operation> hostOperations = log.get(hostid);
			Timestamp lastSeenBySum = sum.getLast(hostid);
			
			// If the summary has never seen operations from this host,
			// add all operations from this host
			if (lastSeenBySum == null || lastSeenBySum.isNullTimestamp()) {
				newerOperations.addAll(hostOperations);
			} else {
				// Add operations that are newer than what the summary has seen
				for (Operation op : hostOperations) {
					Timestamp opTimestamp = op.getTimestamp();
					// If op timestamp is newer than last seen by summary
					// opTimestamp.compare(lastSeenBySum) > 0 means opTimestamp is newer
					if (opTimestamp.compare(lastSeenBySum) > 0) {
						newerOperations.add(op);
					}
				}
			}
		}
		
		return newerOperations;
	}
	
	/**
	 * Removes from the log the operations that have
	 * been acknowledged by all the members
	 * of the group, according to the provided
	 * ackSummary. 
	 * @param ack: ackSummary.
	 */
	public void purgeLog(TimestampMatrix ack){
	}

	/**
	 * equals
	 */
	@Override
	public boolean equals(Object obj) {
		// Check for identity
		if (this == obj) {
			return true;
		}
		
		// Check for null
		if (obj == null) {
			return false;
		}
		
		// Check for correct type
		if (!(obj instanceof Log)) {
			return false;
		}
		
		// Cast to Log
		Log other = (Log) obj;
		
		// Check if both logs have the same structure
		if (this.log == null) {
			return other.log == null;
		}
		
		// Check if they have the same number of hosts
		if (this.log.size() != other.log.size()) {
			return false;
		}
		
		// Check each host's operations list
		for (String hostid : this.log.keySet()) {
			// Check if the other log has this host
			if (!other.log.containsKey(hostid)) {
				return false;
			}
			
			List<Operation> thisOps = this.log.get(hostid);
			List<Operation> otherOps = other.log.get(hostid);
			
			// Check if both lists have the same size
			if (thisOps.size() != otherOps.size()) {
				return false;
			}
			
			// Check each operation in the list
			for (int i = 0; i < thisOps.size(); i++) {
				if (!thisOps.get(i).equals(otherOps.get(i))) {
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
		String name="";
		for(Enumeration<List<Operation>> en=log.elements();
		en.hasMoreElements(); ){
		List<Operation> sublog=en.nextElement();
		for(ListIterator<Operation> en2=sublog.listIterator(); en2.hasNext();){
			name+=en2.next().toString()+"\n";
		}
	}
		
		return name;
	}
}
