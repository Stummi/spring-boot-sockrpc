var client = {}

client.alert = function(msg) {
	alert(msg); 
}

var sock = new SockJS('/ws/test');
var sockRpc = new SockRPC(sock, client);
var server; 

sockRpc.onopen = function(s) {
	server = s;
};

function greetme() {
	var name = document.getElementById('name').value;
	server.greet(name);
}


