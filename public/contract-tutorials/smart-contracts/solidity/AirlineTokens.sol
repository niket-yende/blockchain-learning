// SPDX-License-Identifier: MIT
pragma solidity ^0.8.21;
import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v4.9.3/contracts/token/ERC1155/ERC1155.sol";

contract AirlineTokens is ERC1155 {
    address public governance;
    uint256 public airlineCount;
    
    modifier onlyGovernance() {
        require(msg.sender == governance, "only governance can call this");
        
        _;
    }

    constructor(address governance_) public ERC1155("") {
        governance = governance_;
        airlineCount = 0;
    }
    
    function addNewAirline(uint256 initialSupply) external onlyGovernance {
        airlineCount++;
        uint256 airlineTokenClassId = airlineCount;

        _mint(msg.sender, airlineTokenClassId, initialSupply, "");        
    }
}