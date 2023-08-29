// SPDX-License-Identifier: MIT
pragma solidity ^0.8.21;
import "./Owned.sol"; 

contract Setter is Owned {
    bytes32 public name;

    event NameChangedEvent(bytes32 indexed _name, bytes32 indexed _newName);

    function setNamePaid(bytes32 _newName) public payable returns (bool success){
        require(msg.value > 0, "This operation requires some ether");
        require(_newName != "", "Please provide a valid name");
        require(_newName != name, "newName cannot be same as name");

        name = _newName;

        emit NameChangedEvent(name, _newName);

        return true;
    }

    function setNameFree(bytes32 _newName) public onlyOwner returns (bool success){
        require(_newName != "", "Please provide a valid name");
        require(_newName != name, "newName cannot be same as name");

        name = _newName;

        emit NameChangedEvent(name, _newName);

        return true;
    }

}