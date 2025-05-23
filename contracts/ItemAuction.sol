// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/Ownable.sol";
import "./ItemUpload.sol";

contract ItemAuction is Ownable {
  struct Auction {
    uint256 tokenId;
    address seller;
    uint256 startPrice;
    uint256 currentPrice;
    uint256 startTime;
    uint256 endTime;
    address highestBidder;
    bool isActive;
    bool isSold;
  }

  ItemUpload private _itemContract;
  mapping(uint256 => Auction) private _auctions;
  mapping(uint256 => mapping(address => uint256[])) private _bidHistory;

  event AuctionCreated(
    uint256 indexed tokenId,
    address indexed seller,
    uint256 startPrice,
    uint256 startTime,
    uint256 endTime
  );

  event BidPlaced(
    uint256 indexed tokenId,
    address indexed bidder,
    uint256 amount,
    uint256 timestamp
  );

  event AuctionEnded(
    uint256 indexed tokenId,
    address indexed winner,
    uint256 finalPrice
  );

  constructor(address itemContractAddress) Ownable(msg.sender) {
    _itemContract = ItemUpload(itemContractAddress);
  }

  function createAuction(
    uint256 tokenId,
    uint256 startPrice,
    uint256 duration
  ) public {
    require(_itemContract.ownerOf(tokenId) == msg.sender, "Not token owner");
    require(
      _itemContract.getApproved(tokenId) == address(this),
      "Contract not approved"
    );

    _auctions[tokenId] = Auction({
      tokenId: tokenId,
      seller: msg.sender,
      startPrice: startPrice,
      currentPrice: startPrice,
      startTime: block.timestamp,
      endTime: block.timestamp + duration,
      highestBidder: address(0),
      isActive: true,
      isSold: false
    });

    emit AuctionCreated(
      tokenId,
      msg.sender,
      startPrice,
      block.timestamp,
      block.timestamp + duration
    );
  }

  function placeBid(uint256 tokenId) public payable {
    Auction storage auction = _auctions[tokenId];
    require(auction.isActive, "Auction not active");
    require(block.timestamp < auction.endTime, "Auction ended");
    require(msg.value > auction.currentPrice, "Bid too low");
    require(msg.sender != auction.seller, "Seller cannot bid");

    if (auction.highestBidder != address(0)) {
      payable(auction.highestBidder).transfer(auction.currentPrice);
    }

    auction.currentPrice = msg.value;
    auction.highestBidder = msg.sender;

    _bidHistory[tokenId][msg.sender].push(msg.value);

    emit BidPlaced(tokenId, msg.sender, msg.value, block.timestamp);
  }

  function endAuction(uint256 tokenId) public {
    Auction storage auction = _auctions[tokenId];
    require(auction.isActive, "Auction not active");
    require(
      block.timestamp >= auction.endTime || msg.sender == auction.seller,
      "Cannot end auction yet"
    );

    auction.isActive = false;
    auction.isSold = auction.highestBidder != address(0);

    if (auction.isSold) {
      _itemContract.transferFrom(
        auction.seller,
        auction.highestBidder,
        tokenId
      );
      payable(auction.seller).transfer(auction.currentPrice);

      emit AuctionEnded(tokenId, auction.highestBidder, auction.currentPrice);
    }
  }

  function getAuction(
    uint256 tokenId
  )
    public
    view
    returns (
      address seller,
      uint256 startPrice,
      uint256 currentPrice,
      uint256 startTime,
      uint256 endTime,
      address highestBidder,
      bool isActive,
      bool isSold
    )
  {
    Auction storage auction = _auctions[tokenId];
    return (
      auction.seller,
      auction.startPrice,
      auction.currentPrice,
      auction.startTime,
      auction.endTime,
      auction.highestBidder,
      auction.isActive,
      auction.isSold
    );
  }

  function getBidHistory(
    uint256 tokenId,
    address bidder
  ) public view returns (uint256[] memory) {
    return _bidHistory[tokenId][bidder];
  }
}
