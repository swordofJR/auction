// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/Ownable.sol";
import "./ItemUpload.sol";
import "./ItemAuction.sol";

contract ItemTraceability is Ownable {
  struct Transfer {
    address from;
    address to;
    uint256 timestamp;
    uint256 price;
    string transactionType;
  }

  ItemUpload private _itemContract;
  ItemAuction private _auctionContract;

  mapping(uint256 => Transfer[]) private _transferHistory;
  mapping(uint256 => string[]) private _statusHistory;

  event TransferRecorded(
    uint256 indexed tokenId,
    address indexed from,
    address indexed to,
    uint256 timestamp,
    uint256 price,
    string transactionType
  );

  event StatusRecorded(
    uint256 indexed tokenId,
    string status,
    uint256 timestamp
  );

  constructor(
    address itemContractAddress,
    address auctionContractAddress
  ) Ownable(msg.sender) {
    _itemContract = ItemUpload(itemContractAddress);
    _auctionContract = ItemAuction(auctionContractAddress);
  }

  function recordTransfer(
    uint256 tokenId,
    address from,
    address to,
    uint256 price,
    string memory transactionType
  ) public {
    require(
      msg.sender == address(_auctionContract),
      "Only auction contract can record transfers"
    );

    Transfer memory transfer = Transfer({
      from: from,
      to: to,
      timestamp: block.timestamp,
      price: price,
      transactionType: transactionType
    });

    _transferHistory[tokenId].push(transfer);

    emit TransferRecorded(
      tokenId,
      from,
      to,
      block.timestamp,
      price,
      transactionType
    );
  }

  function recordStatus(uint256 tokenId, string memory status) public {
    require(
      msg.sender == address(_auctionContract) ||
        msg.sender == address(_itemContract),
      "Only item or auction contract can record status"
    );

    _statusHistory[tokenId].push(status);

    emit StatusRecorded(tokenId, status, block.timestamp);
  }

  function getTransferHistory(
    uint256 tokenId
  )
    public
    view
    returns (
      address[] memory fromAddresses,
      address[] memory toAddresses,
      uint256[] memory timestamps,
      uint256[] memory prices,
      string[] memory transactionTypes
    )
  {
    Transfer[] storage transfers = _transferHistory[tokenId];
    uint256 length = transfers.length;

    fromAddresses = new address[](length);
    toAddresses = new address[](length);
    timestamps = new uint256[](length);
    prices = new uint256[](length);
    transactionTypes = new string[](length);

    for (uint256 i = 0; i < length; i++) {
      Transfer storage transfer = transfers[i];
      fromAddresses[i] = transfer.from;
      toAddresses[i] = transfer.to;
      timestamps[i] = transfer.timestamp;
      prices[i] = transfer.price;
      transactionTypes[i] = transfer.transactionType;
    }
  }

  function getStatusHistory(
    uint256 tokenId
  ) public view returns (string[] memory) {
    return _statusHistory[tokenId];
  }

  function getItemCurrentOwner(uint256 tokenId) public view returns (address) {
    return _itemContract.ownerOf(tokenId);
  }

  function getItemInfo(
    uint256 tokenId
  )
    public
    view
    returns (
      string memory title,
      string memory description,
      string memory imageUrl,
      string memory category,
      uint256 registrationTime,
      address owner,
      bool isRegistered,
      string memory ipfsHash
    )
  {
    return _itemContract.getItemInfo(tokenId);
  }
}
