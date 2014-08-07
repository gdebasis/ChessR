/************************************************************************************************************
Chess Fen Viewer
Copyright (C) 2007  DTHMLGoodies.com, Alf Magne Kalleland

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

Dhtmlgoodies.com., hereby disclaims all copyright interest in this script
written by Alf Magne Kalleland.

Alf Magne Kalleland, 2007
Owner of DHTMLgoodies.com


************************************************************************************************************/	

var DHTMLGoodies = new Object();
if(!String.trim)String.prototype.trim = function() { return this.replace(/^\s+|\s+$/, ''); };

/* The widget */
DHTMLGoodies.ChessFen = function(props)
{
	var divBoard;
	var cssPath;
	var parentRef;
	var squareSize;
	var imageFolder;
	var isOldMSIE;
	var boardLabels;
	var flipBoardWhenBlackToMove;
	
	var pieceType;
	
	this.pieceType = 'cases';
	this.squareSize = 45;
	this.isOldMSIE = (navigator.userAgent.toLowerCase().match(/msie\s[0-6]/gi))?true:false;
	this.isOpera = (navigator.userAgent.toLowerCase().indexOf('opera')>=0)?true:false;

	this.cssPath = 'css/chess.css';
	this.parentRef = document.body;
	this.imageFolder = 'images/';
	this.boardLabels = true;
	this.flipBoardWhenBlackToMove = true;
	
	if(props)this.__setInitProps(props);
	this.init();
}


DHTMLGoodies.ChessFen.prototype = {
	__setInitProps : function(props)
	{
		if(props.cssPath)this.cssPath = props.cssPath;	
		if(props.imageFolder)this.imageFolder = props.imageFolder;	
		if(props.squareSize)this.squareSize = props.squareSize;	
		if(props.boardLabels || props.boardLabels===false)this.boardLabels = props.boardLabels;	
		if(props.flipBoardWhenBlackToMove || props.flipBoardWhenBlackToMove===false)this.flipBoardWhenBlackToMove = props.flipBoardWhenBlackToMove;	
		if(props.pieceType)this.pieceType = props.pieceType;	
	}
	,
	setSquareSize : function(squareSize)
	{
		this.squareSize = squareSize;
	}
	,
	setPieceType : function(pieceType)
	{
		this.pieceType = pieceType;
	}
	,	
	init : function()
	{
		this.__loadCss(this.cssPath);
	}
	,
	setFlipBoardWhenBlackToMove : function(flipBoardWhenBlackToMove)
	{
		this.flipBoardWhenBlackToMove = flipBoardWhenBlackToMove;
	}
	,
	getWhoToMove : function()
	{
		return this.whoToMove;
	}
	,
	setBoardLabels : function(boardLabels)
	{
		this.boardLabels = boardLabels;
	}
	,
	__setWhoToMove : function(fenString)
	{
		var items = fenString.split(/\s/g);
		this.whoToMove = items[1].trim();	
	}
	,
	loadFen : function(fenString,element)
	{
		this.__setWhoToMove(fenString);
		
		element = this.__getEl(element);
		element.innerHTML = '';
		var boardOuter = document.createElement('DIV');	
		boardOuter.className = 'ChessBoard' + this.squareSize;
		boardOuter.style.position = 'relative';
		
		var board = document.createElement('DIV');		
		board.className = 'ChessBoardInner' + this.squareSize;
			
		
		if(this.boardLabels){
			this.__addBoardLabels(boardOuter);
			boardOuter.appendChild(board);
			board.style.position = 'absolute';
			board.style.top='0px';
			board.style.right='0px';
			element.appendChild(boardOuter);
		}else{
			board.style.position = 'relative';	
			element.appendChild(board);
		}
		this.__loadFen(fenString,board);
		
	}
	,
	__addBoardLabels : function(boardOuter)
	{
		var letters = 'ABCDEFGH';
		for(var no=1;no<=8;no++){
			var file = document.createElement('DIV');
			file.style.position = 'absolute';
			file.style.right = ((8-no) * this.squareSize) + 'px';	
			file.style.bottom = '0px';
			file.innerHTML = letters.substr((no-1),1);
			file.style.textAlign = 'center';
			file.style.width = this.squareSize + 'px';
			boardOuter.appendChild(file);
			file.className = 'ChessBoardLabel ChessBoardLabel'+this.squareSize;
			
			var rank = document.createElement('DIV');
			rank.style.position = 'absolute';
			rank.style.left = '0px';	
			rank.style.top = ((8-no) * this.squareSize) + 'px';	;
			rank.innerHTML = no;
			rank.style.height = this.squareSize + 'px';
			rank.style.lineHeight = this.squareSize + 'px';
			boardOuter.appendChild(rank);
			rank.className = 'ChessBoardLabel ChessBoardLabel'+this.squareSize;
			
			if(this.whoToMove=='b' && this.flipBoardWhenBlackToMove){
				rank.innerHTML = 9-no;	
				file.innerHTML = letters.substr((8-no),1);
			}
		}
		
	}
	,
	// Load Forsyth-Edwards Notation (FEN)
	__loadFen : function(fenString,boardEl)
	{
		var items = fenString.split(/\s/g);
		var pieces = items[0];
		
		var currentCol = 0;
		for(var no=0;no<pieces.length;no++){
			var character = pieces.substr(no,1);
			
			if(character.match(/[A-Z]/i)){	/* White pieces */	
				var boardPos = this.__getBoardPosByCol(currentCol);
				var piece = document.createElement('DIV');
				piece.style.position = 'absolute';
				piece.style.left = boardPos.x + 'px';
				piece.style.top = boardPos.y + 'px';				
			
				if(character.match(/[A-Z]/)){	/* White pieces */						
					var color = 'w';
				}
				if(character.match(/[a-z]/)){	/* Black pieces */
					var color = 'b';
				}
				var img = document.createElement('IMG');
				img.src = this.imageFolder + this.pieceType + this.squareSize  + color + character.toLowerCase() + '.png';				
				self.status = this.imageFolder + this.pieceType + this.squareSize  + color + character.toLowerCase() + '.png';;
				piece.appendChild(img);
				piece.className = 'ChessPiece' + this.squareSize;
				boardEl.appendChild(piece);
				currentCol++;
				if(this.isOldMSIE && !this.isOpera)this.correctPng(img);			
			}
			if(character.match(/[0-8]/))currentCol+=character/1;
		}
		
		
	}
	,
	/* Starting from the top */
	/* 1-64 */		
	__getBoardPosByCol : function(col)
	{
		var rank = 0;
		while(col>=8){
			rank++;
			col-=8;
		}
		var retArray = new Object();
		
		if(this.whoToMove=='b' && this.flipBoardWhenBlackToMove){
			col = 7-col;
			rank = 7-rank;			
		}
		
		retArray.x = col* this.squareSize;
		retArray.y = rank * this.squareSize;
		return retArray;
		
		
	}
	,
	__loadCss : function(cssFile)
	{
		var lt = document.createElement('LINK');
		lt.href = cssFile + '?rand=' + Math.random();			
		lt.rel = 'stylesheet';
		lt.media = 'screen';
		lt.type = 'text/css';
		document.getElementsByTagName('HEAD')[0].appendChild(lt);			
	}		
	,
	__getEl : function(elRef)
	{
		if(typeof elRef=='string'){
			if(document.getElementById(elRef))return document.getElementById(elRef);
			if(document.forms[elRef])return document.forms[elRef];
			if(document[elRef])return document[elRef];
			if(window[elRef])return window[elRef];
		}
		return elRef;	// Return original ref.		
	}
	,
	correctPng : function(el)
	{
		el = this.__getEl(el);	
		var img = el;
		var width = img.width;
		var height = img.height;
		var html = '<span style="position:absolute;display:inline-block;filter:progid:DXImageTransform.Microsoft.AlphaImageLoader(src=\'' + img.src + '\',sizingMethod=\'scale\');width:' + width + ';height:' + height + '"></span>';
		img.outerHTML = html;	
					
	}	
}