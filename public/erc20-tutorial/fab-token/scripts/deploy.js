const hre = require("hardhat");

async function main() {
  const FabToken = await hre.ethers.getContractFactory("FabToken");
  const fabToken = await FabToken.deploy(100000000, 50);

  await fabToken.deployed();

  console.log("Fab Token deployed: ", fabToken.address);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
