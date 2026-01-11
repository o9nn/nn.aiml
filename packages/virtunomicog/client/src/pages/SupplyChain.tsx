import { useState } from "react";
import { trpc } from "@/lib/trpc";
import GameLayout from "@/components/GameLayout";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Loader2, Truck, Package, ArrowRight, MapPin, Clock, DollarSign, Send, Building2, AlertTriangle, CheckCircle2 } from "lucide-react";
import { useToast } from "@/hooks/use-toast";

export default function SupplyChain() {
  const { toast } = useToast();
  const [fromUnitId, setFromUnitId] = useState<string>("");
  const [toUnitId, setToUnitId] = useState<string>("");
  const [resourceTypeId, setResourceTypeId] = useState<string>("");
  const [quantity, setQuantity] = useState(100);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string>("all");

  const { data: units, isLoading: unitsLoading } = trpc.businessUnit.list.useQuery();
  const { data: resources } = trpc.resource.list.useQuery();
  const { data: routes } = trpc.supplyChain.routes.useQuery();
  const { data: shipments, refetch: refetchShipments, isLoading: shipmentsLoading } = trpc.supplyChain.shipments.useQuery(
    statusFilter === "all" ? {} : { status: statusFilter as "pending" | "in_transit" | "delivered" | "delayed" | "lost" }
  );
  const { data: cities } = trpc.city.list.useQuery();

  // Get inventory for source unit
  const { data: sourceInventory } = trpc.businessUnit.inventory.useQuery(
    { unitId: parseInt(fromUnitId) },
    { enabled: !!fromUnitId }
  );

  // Calculate shipping cost
  const { data: shippingCost } = trpc.supplyChain.calculateShipping.useQuery(
    {
      fromUnitId: parseInt(fromUnitId),
      toUnitId: parseInt(toUnitId),
      quantity,
    },
    { enabled: !!fromUnitId && !!toUnitId && quantity > 0 }
  );

  const shipGoods = trpc.supplyChain.ship.useMutation({
    onSuccess: () => {
      toast({
        title: "Shipment Created",
        description: "Your goods are on their way!",
      });
      setDialogOpen(false);
      refetchShipments();
      setFromUnitId("");
      setToUnitId("");
      setResourceTypeId("");
      setQuantity(100);
    },
    onError: (error) => {
      toast({
        title: "Shipment Failed",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const getCityName = (cityId: number) => {
    return cities?.find((c) => c.id === cityId)?.name || "Unknown";
  };

  const getUnitName = (unitId: number) => {
    return units?.find((u) => u.id === unitId)?.name || "Unknown";
  };

  const getResourceName = (id: number) => {
    return resources?.find((r) => r.id === id)?.name || "Unknown";
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "pending":
        return <Badge variant="secondary"><Clock className="w-3 h-3 mr-1" /> Pending</Badge>;
      case "in_transit":
        return <Badge className="bg-blue-500/20 text-blue-500 border-blue-500/50"><Truck className="w-3 h-3 mr-1" /> In Transit</Badge>;
      case "delivered":
        return <Badge className="bg-green-500/20 text-green-500 border-green-500/50"><CheckCircle2 className="w-3 h-3 mr-1" /> Delivered</Badge>;
      case "delayed":
        return <Badge className="bg-yellow-500/20 text-yellow-500 border-yellow-500/50"><AlertTriangle className="w-3 h-3 mr-1" /> Delayed</Badge>;
      case "lost":
        return <Badge variant="destructive">Lost</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  const fromUnit = units?.find((u) => u.id === parseInt(fromUnitId));
  const toUnit = units?.find((u) => u.id === parseInt(toUnitId));

  const availableInventory = sourceInventory?.find(
    (inv) => inv.inventory.resourceTypeId === parseInt(resourceTypeId)
  );

  const handleShip = () => {
    if (!fromUnitId || !toUnitId || !resourceTypeId) return;
    shipGoods.mutate({
      fromUnitId: parseInt(fromUnitId),
      toUnitId: parseInt(toUnitId),
      resourceTypeId: parseInt(resourceTypeId),
      quantity,
    });
  };

  if (unitsLoading) {
    return (
      <GameLayout>
        <div className="flex items-center justify-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
        </div>
      </GameLayout>
    );
  }

  return (
    <GameLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">Supply Chain</h1>
            <p className="text-muted-foreground">Manage logistics and shipments between your facilities</p>
          </div>
          <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
            <DialogTrigger asChild>
              <Button>
                <Send className="w-4 h-4 mr-2" />
                New Shipment
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-2xl">
              <DialogHeader>
                <DialogTitle>Create Shipment</DialogTitle>
                <DialogDescription>
                  Transfer goods between your business units
                </DialogDescription>
              </DialogHeader>
              <div className="grid gap-4 py-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>From Unit</Label>
                    <Select value={fromUnitId} onValueChange={setFromUnitId}>
                      <SelectTrigger>
                        <SelectValue placeholder="Select source" />
                      </SelectTrigger>
                      <SelectContent>
                        {units?.map((unit) => (
                          <SelectItem key={unit.id} value={unit.id.toString()}>
                            {unit.name} ({getCityName(unit.cityId)})
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label>To Unit</Label>
                    <Select value={toUnitId} onValueChange={setToUnitId}>
                      <SelectTrigger>
                        <SelectValue placeholder="Select destination" />
                      </SelectTrigger>
                      <SelectContent>
                        {units?.filter(u => u.id !== parseInt(fromUnitId)).map((unit) => (
                          <SelectItem key={unit.id} value={unit.id.toString()}>
                            {unit.name} ({getCityName(unit.cityId)})
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>Resource</Label>
                    <Select value={resourceTypeId} onValueChange={setResourceTypeId}>
                      <SelectTrigger>
                        <SelectValue placeholder="Select resource" />
                      </SelectTrigger>
                      <SelectContent>
                        {sourceInventory?.filter(inv => parseFloat(inv.inventory.quantity) > 0).map((inv) => (
                          <SelectItem key={inv.resourceType?.id} value={inv.resourceType?.id.toString() || ""}>
                            {inv.resourceType?.name} ({parseFloat(inv.inventory.quantity).toFixed(0)} available)
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label>Quantity</Label>
                    <Input
                      type="number"
                      min={1}
                      max={availableInventory ? parseFloat(availableInventory.inventory.quantity) : undefined}
                      value={quantity}
                      onChange={(e) => setQuantity(parseInt(e.target.value) || 1)}
                    />
                  </div>
                </div>

                {fromUnit && toUnit && shippingCost && (
                  <div className="p-4 rounded-lg bg-muted">
                    <h4 className="font-medium mb-3">Shipment Preview</h4>
                    <div className="flex items-center gap-2 mb-3">
                      <div className="flex items-center gap-2">
                        <Building2 className="w-4 h-4 text-muted-foreground" />
                        <span className="text-sm">{fromUnit.name}</span>
                        <span className="text-xs text-muted-foreground">({getCityName(fromUnit.cityId)})</span>
                      </div>
                      <ArrowRight className="w-4 h-4 text-muted-foreground" />
                      <div className="flex items-center gap-2">
                        <Building2 className="w-4 h-4 text-muted-foreground" />
                        <span className="text-sm">{toUnit.name}</span>
                        <span className="text-xs text-muted-foreground">({getCityName(toUnit.cityId)})</span>
                      </div>
                    </div>
                    <div className="grid grid-cols-3 gap-4 text-sm">
                      <div>
                        <span className="text-muted-foreground">Transit Time:</span>
                        <p className="font-medium flex items-center gap-1">
                          <Clock className="w-4 h-4" />
                          {shippingCost.transitTime} turn(s)
                        </p>
                      </div>
                      <div>
                        <span className="text-muted-foreground">Shipping Cost:</span>
                        <p className="font-medium flex items-center gap-1">
                          <DollarSign className="w-4 h-4" />
                          ${shippingCost.cost.toLocaleString()}
                        </p>
                      </div>
                      <div>
                        <span className="text-muted-foreground">Quantity:</span>
                        <p className="font-medium flex items-center gap-1">
                          <Package className="w-4 h-4" />
                          {quantity} units
                        </p>
                      </div>
                    </div>
                  </div>
                )}
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setDialogOpen(false)}>
                  Cancel
                </Button>
                <Button
                  onClick={handleShip}
                  disabled={!fromUnitId || !toUnitId || !resourceTypeId || shipGoods.isPending}
                >
                  {shipGoods.isPending ? (
                    <>
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                      Shipping...
                    </>
                  ) : (
                    <>
                      <Truck className="w-4 h-4 mr-2" />
                      Ship Goods
                    </>
                  )}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>

        <Tabs defaultValue="shipments">
          <TabsList>
            <TabsTrigger value="shipments">Shipments</TabsTrigger>
            <TabsTrigger value="routes">Routes</TabsTrigger>
          </TabsList>

          <TabsContent value="shipments" className="space-y-4">
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div>
                    <CardTitle>Active Shipments</CardTitle>
                    <CardDescription>Track your goods in transit</CardDescription>
                  </div>
                  <Select value={statusFilter} onValueChange={setStatusFilter}>
                    <SelectTrigger className="w-40">
                      <SelectValue placeholder="Filter status" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Shipments</SelectItem>
                      <SelectItem value="pending">Pending</SelectItem>
                      <SelectItem value="in_transit">In Transit</SelectItem>
                      <SelectItem value="delivered">Delivered</SelectItem>
                      <SelectItem value="delayed">Delayed</SelectItem>
                      <SelectItem value="lost">Lost</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </CardHeader>
              <CardContent>
                {shipmentsLoading ? (
                  <div className="flex items-center justify-center py-8">
                    <Loader2 className="w-6 h-6 animate-spin text-primary" />
                  </div>
                ) : shipments && shipments.length > 0 ? (
                  <div className="space-y-4">
                    {shipments.map((shipment) => (
                      <div
                        key={shipment.id}
                        className="p-4 rounded-lg border bg-card"
                      >
                        <div className="flex items-start justify-between mb-3">
                          <div className="flex items-center gap-2">
                            {getStatusBadge(shipment.status)}
                            <span className="text-sm text-muted-foreground">
                              #{shipment.id}
                            </span>
                          </div>
                          <div className="text-right text-sm">
                            <p className="text-muted-foreground">Shipping Cost</p>
                            <p className="font-medium">${parseFloat(shipment.shippingCost).toLocaleString()}</p>
                          </div>
                        </div>

                        <div className="flex items-center gap-2 mb-3">
                          <div className="flex-1">
                            <p className="text-sm text-muted-foreground">From</p>
                            <p className="font-medium">{getUnitName(shipment.fromUnitId)}</p>
                          </div>
                          <ArrowRight className="w-5 h-5 text-muted-foreground" />
                          <div className="flex-1">
                            <p className="text-sm text-muted-foreground">To</p>
                            <p className="font-medium">{getUnitName(shipment.toUnitId)}</p>
                          </div>
                        </div>

                        <div className="flex items-center justify-between text-sm">
                          <div className="flex items-center gap-4">
                            <div className="flex items-center gap-1">
                              <Package className="w-4 h-4 text-muted-foreground" />
                              <span>{parseFloat(shipment.quantity).toFixed(0)} {getResourceName(shipment.resourceTypeId)}</span>
                            </div>
                            <div className="flex items-center gap-1">
                              <Clock className="w-4 h-4 text-muted-foreground" />
                              <span>ETA: Turn {shipment.expectedArrivalTurn}</span>
                            </div>
                          </div>
                          <Badge variant="outline">
                            Quality: {(parseFloat(shipment.quality) * 100).toFixed(0)}%
                          </Badge>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center py-12">
                    <Truck className="w-12 h-12 text-muted-foreground mb-4" />
                    <h3 className="text-lg font-semibold mb-2">No Shipments</h3>
                    <p className="text-muted-foreground text-center mb-4">
                      Create a shipment to start moving goods between your facilities.
                    </p>
                    <Button onClick={() => setDialogOpen(true)}>
                      <Send className="w-4 h-4 mr-2" />
                      Create Shipment
                    </Button>
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="routes" className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle>Available Routes</CardTitle>
                <CardDescription>Shipping routes between cities</CardDescription>
              </CardHeader>
              <CardContent>
                {routes && routes.length > 0 ? (
                  <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
                    {routes.map((route) => (
                      <div
                        key={route.id}
                        className="p-4 rounded-lg border bg-card"
                      >
                        <div className="flex items-center gap-2 mb-2">
                          <MapPin className="w-4 h-4 text-muted-foreground" />
                          <span className="font-medium">{getCityName(route.fromCityId)}</span>
                          <ArrowRight className="w-4 h-4 text-muted-foreground" />
                          <span className="font-medium">{getCityName(route.toCityId)}</span>
                        </div>
                        <div className="grid grid-cols-2 gap-2 text-sm">
                          <div>
                            <span className="text-muted-foreground">Distance:</span>
                            <p className="font-medium">{parseFloat(route.distance).toFixed(0)} km</p>
                          </div>
                          <div>
                            <span className="text-muted-foreground">Transit Time:</span>
                            <p className="font-medium">{route.transitTime} turn(s)</p>
                          </div>
                          <div>
                            <span className="text-muted-foreground">Base Cost:</span>
                            <p className="font-medium">${parseFloat(route.baseShippingCost).toFixed(2)}/unit</p>
                          </div>
                          <div>
                            <span className="text-muted-foreground">Reliability:</span>
                            <p className="font-medium">{(parseFloat(route.reliability) * 100).toFixed(0)}%</p>
                          </div>
                        </div>
                        <Badge variant="outline" className="mt-2 capitalize">
                          {route.routeType}
                        </Badge>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-muted-foreground text-center py-8">
                    No routes available. Routes are created automatically between cities.
                  </p>
                )}
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </GameLayout>
  );
}
