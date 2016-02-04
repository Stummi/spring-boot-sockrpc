function SockRPC(sock, client) {
	this.onsockopen = function() {};
	this.onopen = function(srv) {};
	this.onclose = function() {};
	this.server = {};

	this.invoke = function(name, args) {
		sock.send("invoke:"+JSON.stringify({
			methodName: name,
			args: args
		}));
	}
	
	var $this = this;
	
	sock.onopen = function() {
		$this.onsockopen();
	};
	
	sock.onmessage = function(e) {
		var msg = e.data;
		var splitPos = msg.indexOf(':');
		var cmd = msg.slice(0,splitPos);
		var arg = msg.slice(splitPos+1);
		
		if(cmd == "export") {
			var funcs = arg.split(",");
			for (var i = 0; i < funcs.length; ++i) {
				console.log("export: " + funcs[i]);
				var funcName = funcs[i];
				$this.server[funcName] = function(args) {
					$this.invoke(funcName, args);
				};
			}
			$this.onopen($this.server);
		} else if(cmd == "invoke") {
			var invokeData = JSON.parse(arg);
			var func = invokeData.methodName;
			var args = invokeData.args;
			
			if(typeof(client[func]) === "function") {
				client[func](args);
			}
		}
		
	};
}