/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package distributeddeschallenge.server;

/**
 *
 * @author franc
 */
public class VolunteerDescriptor {
	int volunteer_number;
	int key_block_id;
	boolean live;

	public VolunteerDescriptor(int volunteer_number) {
		this.volunteer_number = volunteer_number;
		this.key_block_id = -1;
		this.live = true;
	}

	public int getVolunteer_number() {
		return volunteer_number;
	}

	public void setVolunteer_number(int volunteer_number) {
		this.volunteer_number = volunteer_number;
	}

	public int getKey_block_id() {
		return key_block_id;
	}

	public void setKey_block_id(int key_block_id) {
		this.key_block_id = key_block_id;
	}

	public boolean isLive() {
		return live;
	}

	public void setLive(boolean live) {
		this.live = live;
	}	
}

