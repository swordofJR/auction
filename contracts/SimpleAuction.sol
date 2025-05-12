// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title SimpleAuction
 * @dev Simple auction contract that allows auction creation, bidding, and settlement
 */
contract SimpleAuction {
  // Auction struct
  struct Auction {
    address creator; // Auction creator
    address payable beneficiary; // Beneficiary (final recipient)
    uint256 startTime; // Start time
    uint256 endTime; // End time
    uint256 highestBid; // Highest bid
    address highestBidder; // Highest bidder
    bool ended; // Whether ended
    string title; // Auction title
    string description; // Auction description
    string imageUrl; // Image URL
  }

  // Mapping from auction ID to auction object
  mapping(uint256 => Auction) public auctions;

  // Mapping from auction ID to previous bidder's bid
  mapping(uint256 => mapping(address => uint256)) public pendingReturns;

  // Total number of auctions created
  uint256 public auctionCount;

  // Active auction ID array
  uint256[] public activeAuctions;

  // Events
  event AuctionCreated(
    uint256 auctionId,
    address creator,
    address beneficiary,
    uint256 startTime,
    uint256 endTime
  );
  event HighestBidIncreased(uint256 auctionId, address bidder, uint256 amount);
  event AuctionEnded(uint256 auctionId, address winner, uint256 amount);
  event BidRefunded(uint256 auctionId, address bidder, uint256 amount);

  /**
   * @dev Create a new auction
   * @param _beneficiary Beneficiary address
   * @param _duration Auction duration (seconds)
   * @param _startingPrice Starting price
   * @param _title Auction title
   * @param _description Auction description
   * @param _imageUrl Image URL
   */
  function createAuction(
    address payable _beneficiary,
    uint256 _duration,
    uint256 _startingPrice,
    string memory _title,
    string memory _description,
    string memory _imageUrl
  ) external returns (uint256) {
    require(_beneficiary != address(0), "Beneficiary address cannot be zero");
    require(_duration > 0, "Auction duration must be greater than 0");

    uint256 newAuctionId = auctionCount;
    auctionCount++;

    Auction storage auction = auctions[newAuctionId];
    auction.creator = msg.sender;
    auction.beneficiary = _beneficiary;
    auction.startTime = block.timestamp;
    auction.endTime = block.timestamp + _duration;
    auction.highestBid = _startingPrice;
    auction.highestBidder = address(0);
    auction.ended = false;
    auction.title = _title;
    auction.description = _description;
    auction.imageUrl = _imageUrl;

    activeAuctions.push(newAuctionId);

    emit AuctionCreated(
      newAuctionId,
      msg.sender,
      _beneficiary,
      auction.startTime,
      auction.endTime
    );

    return newAuctionId;
  }

  /**
   * @dev Get specific auction information
   */
  function getAuction(
    uint256 auctionId
  )
    external
    view
    returns (
      address creator,
      address payable beneficiary,
      uint256 startTime,
      uint256 endTime,
      uint256 highestBid,
      address highestBidder,
      bool ended,
      string memory title,
      string memory description,
      string memory imageUrl
    )
  {
    require(auctionId < auctionCount, "Auction does not exist");
    Auction storage auction = auctions[auctionId];

    return (
      auction.creator,
      auction.beneficiary,
      auction.startTime,
      auction.endTime,
      auction.highestBid,
      auction.highestBidder,
      auction.ended,
      auction.title,
      auction.description,
      auction.imageUrl
    );
  }

  /**
   * @dev Get all active auction IDs
   */
  function getActiveAuctions() external view returns (uint256[] memory) {
    return activeAuctions;
  }

  /**
   * @dev Place a bid
   */
  function placeBid(uint256 auctionId) external payable {
    require(auctionId < auctionCount, "Auction does not exist");
    Auction storage auction = auctions[auctionId];

    require(block.timestamp <= auction.endTime, "Auction has ended");
    require(!auction.ended, "Auction has been manually ended");
    require(
      msg.value > auction.highestBid,
      "Bid must be higher than current highest bid"
    );

    // If there is already a bid, record the previous highest bidder's bid for refund
    if (auction.highestBidder != address(0)) {
      pendingReturns[auctionId][auction.highestBidder] += auction.highestBid;
    }

    // Update highest bid and bidder
    auction.highestBid = msg.value;
    auction.highestBidder = msg.sender;

    emit HighestBidIncreased(auctionId, msg.sender, msg.value);
  }

  /**
   * @dev Withdraw previous bid
   */
  function withdraw(uint256 auctionId) external {
    require(auctionId < auctionCount, "Auction does not exist");

    uint256 amount = pendingReturns[auctionId][msg.sender];
    require(amount > 0, "No amount to withdraw");

    // Set amount to 0 first to prevent reentrancy attacks
    pendingReturns[auctionId][msg.sender] = 0;

    // Send refund
    (bool success, ) = msg.sender.call{value: amount}("");
    require(success, "Refund failed");

    emit BidRefunded(auctionId, msg.sender, amount);
  }

  /**
   * @dev End auction
   */
  function endAuction(uint256 auctionId) external {
    require(auctionId < auctionCount, "Auction does not exist");
    Auction storage auction = auctions[auctionId];

    // Check if caller is auction creator or auction has passed end time
    require(
      msg.sender == auction.creator || block.timestamp > auction.endTime,
      "Only creator can end auction before end time"
    );
    require(!auction.ended, "Auction has already ended");

    auction.ended = true;

    // Remove from active auctions
    _removeFromActiveAuctions(auctionId);

    // If there is a bid, send funds to beneficiary
    if (auction.highestBidder != address(0)) {
      auction.beneficiary.transfer(auction.highestBid);
    }

    emit AuctionEnded(auctionId, auction.highestBidder, auction.highestBid);
  }

  /**
   * @dev Check amount user can withdraw from a specific auction
   */
  function pendingReturn(
    uint256 auctionId,
    address bidder
  ) external view returns (uint256) {
    return pendingReturns[auctionId][bidder];
  }

  /**
   * @dev Remove auction from active auctions
   */
  function _removeFromActiveAuctions(uint256 auctionId) private {
    for (uint i = 0; i < activeAuctions.length; i++) {
      if (activeAuctions[i] == auctionId) {
        activeAuctions[i] = activeAuctions[activeAuctions.length - 1];
        activeAuctions.pop();
        break;
      }
    }
  }
}
