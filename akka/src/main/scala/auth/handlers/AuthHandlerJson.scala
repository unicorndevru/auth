package auth.handlers

import utils.http.json.PlayJsonSupport

trait AuthHandlerJson extends PlayJsonSupport with AuthJsonWrites with AuthJsonReads