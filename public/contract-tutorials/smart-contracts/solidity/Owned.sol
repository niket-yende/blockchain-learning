// SPDX-License-Identifier: MIT
pragma solidity ^0.8.21;

contract Owned {
    address owner;

    constructor() {
        owner = msg.sender;
    }

    modifier onlyOwner {
        require(owner == msg.sender, "Only owner can execute");
        _;
    }

    event LogOwnerEvent(address indexed sender, address indexed newOwner);

    function setOwner(address newOwner) public onlyOwner returns (bool success){
       require(newOwner != address(0), "Address cant be 0 address");
       require(newOwner != owner, "Provide different owner");     

       owner = newOwner;

       emit LogOwnerEvent(msg.sender, newOwner); 

       return true;
    }

    function getOwner() public view returns (address _owner){
        return owner;
    }

}