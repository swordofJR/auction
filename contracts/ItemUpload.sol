// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract ItemUpload is ERC721, Ownable {
  struct Item {
    string title;
    string description;
    string imageUrl;
    string category;
    uint256 registrationTime;
    address owner;
    bool isRegistered;
    string ipfsHash;
  }

  mapping(uint256 => Item) private _items;
  uint256 private _tokenIdCounter;

  event ItemRegistered(
    uint256 indexed tokenId,
    string title,
    string category,
    address owner,
    string ipfsHash
  );

  constructor() ERC721("ItemUpload", "ITEM") Ownable(msg.sender) {}

  function registerItem(
    string memory title,
    string memory description,
    string memory imageUrl,
    string memory category,
    string memory ipfsHash
  ) public returns (uint256) {
    uint256 tokenId = _tokenIdCounter;
    _tokenIdCounter++;

    _items[tokenId] = Item({
      title: title,
      description: description,
      imageUrl: imageUrl,
      category: category,
      registrationTime: block.timestamp,
      owner: msg.sender,
      isRegistered: true,
      ipfsHash: ipfsHash
    });

    _mint(msg.sender, tokenId);

    emit ItemRegistered(tokenId, title, category, msg.sender, ipfsHash);

    return tokenId;
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
    require(_exists(tokenId), "Item does not exist");
    Item storage item = _items[tokenId];
    return (
      item.title,
      item.description,
      item.imageUrl,
      item.category,
      item.registrationTime,
      item.owner,
      item.isRegistered,
      item.ipfsHash
    );
  }

  function getUserItems(address user) public view returns (uint256[] memory) {
    uint256 balance = balanceOf(user);
    uint256[] memory tokenIds = new uint256[](balance);
    uint256 counter = 0;

    for (uint256 i = 0; i < _tokenIdCounter; i++) {
      if (_exists(i) && ownerOf(i) == user) {
        tokenIds[counter] = i;
        counter++;
      }
    }

    return tokenIds;
  }
}
