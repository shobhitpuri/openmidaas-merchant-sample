root = exports ? this

class gregsts
  $ = jQuery
  me = this

  constructor: ->
    me = this

  displayCheckout: (cart_data) ->
    request = $.post '/checkout', cart_data
    request.done (data) ->
      img = $ '<img />'
      img.attr 'src', data.url
      $('#checkout').empty().append(img)
      me.listen '/listen'

    request.fail (jqXHR, status) ->
      alert 'error posting data:' + status
      console.log jqXHR
  
  listen: (url) -> 
    if !!window.EventSource
      console.log 'connecting to event source:' + url

      source = new EventSource url
      msg_cb = (e) ->
        if e.data == "'done'"
          console.log e.data
          source.close()
          document.location.href = '/fulfill'
          

      open_cb = (e) ->
      error_cb = (e) ->
        console.log "error"
        console.log e
        if e.readyState == EventSource.CLOSED
          console.log "closed"
        else
          source.close()
      	

      source.addEventListener 'message', msg_cb, false

      source.addEventListener 'open', open_cb, false
 
      source.addEventListener 'error', error_cb , false

  hideCheckout: ->
    true

root.gregsts = new gregsts
