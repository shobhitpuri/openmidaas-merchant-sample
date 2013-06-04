root = exports ? this

class gregsts
  $ = jQuery
  me = this
  source = null
  
  constructor: ->
    me = this

  displayCheckout: (cart_data) ->      
    $('#checkout').show()	
    $('#checkout').empty().append('Processing')
    request = $.post '/checkout', cart_data
    request.done (data) ->
      img = $ '<img />'
      img.attr 'src', data.img_url
      urltxt = $ '<div />'
      urltxt.addClass 'alturl'
      urltxt.text data.url
      $('#checkout').empty().append(img).append(urltxt).
      console?.log(data.url)

    request.fail (jqXHR, status) ->
      $('#checkout').empty.append('error processing checkout')
      console?.log jqXHR
  
  listen: (url) -> 
    if !!window.EventSource
      console.log 'connecting to event source:' + url

      source?.close()
      source = new EventSource url
      msg_cb = (e) ->
        if e.data == "'done'"
          console.log e.data
          source.close()
          document.location.href = '/fulfill'
        if e.data == "'expired'"
          console.log e.data
          source.close()
          me.listen(url)
          
      open_cb = (e) ->
      	console?.log 'opened new connection'
      error_cb = (e) ->
        console?.log "error"
        console?.log e
        if e.readyState == EventSource.CLOSED
          console?.log "closed"
        else
          source.close()
      	
      source.addEventListener 'message', msg_cb, false
      source.addEventListener 'open', open_cb, false
      source.addEventListener 'error', error_cb , false

  ignore: ->
    if source? and source.readyState != EventSource.CLOSED
      console?.log 'killing connection'
      source?.close()
      source = null
  

  hideCheckout: ->
    $('#checkout').hide()

  	

root.gregsts = new gregsts
